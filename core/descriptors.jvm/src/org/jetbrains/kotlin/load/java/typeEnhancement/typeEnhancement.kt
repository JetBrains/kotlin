/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.JavaResolverSettings
import org.jetbrains.kotlin.load.java.lazy.types.RawTypeImpl
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier.MUTABLE
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier.READ_ONLY
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier.NOT_NULL
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier.NULLABLE
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.createProjection
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

fun KotlinType.hasEnhancedNullability(): Boolean =
    SimpleClassicTypeSystemContext.hasEnhancedNullability(this)

class JavaTypeEnhancement(private val javaResolverSettings: JavaResolverSettings) {
    private class Result(val type: KotlinType?, val subtreeSize: Int)

    private class SimpleResult(val type: SimpleType?, val subtreeSize: Int, val forWarnings: Boolean)

    // The index in the lambda is the position of the type component:
    // Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
    // which corresponds to the left-to-right breadth-first walk of the tree representation of the type.
    // For flexible types, both bounds are indexed in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`.
    fun KotlinType.enhance(qualifiers: (Int) -> JavaTypeQualifiers, isSuperTypesEnhancement: Boolean = false) =
        unwrap().enhancePossiblyFlexible(qualifiers, 0, isSuperTypesEnhancement).type

    private fun UnwrappedType.enhancePossiblyFlexible(
        qualifiers: (Int) -> JavaTypeQualifiers,
        index: Int,
        isSuperTypesEnhancement: Boolean = false
    ): Result {
        if (isError) return Result(null, 1)
        return when (this) {
            is FlexibleType -> {
                val isRawType = this is RawType
                val lowerResult = lowerBound.enhanceInflexible(
                    qualifiers, index, TypeComponentPosition.FLEXIBLE_LOWER, isRawType, isSuperTypesEnhancement
                )
                val upperResult = upperBound.enhanceInflexible(
                    qualifiers, index, TypeComponentPosition.FLEXIBLE_UPPER, isRawType, isSuperTypesEnhancement
                )
                assert(lowerResult.subtreeSize == upperResult.subtreeSize) {
                    "Different tree sizes of bounds: " +
                            "lower = ($lowerBound, ${lowerResult.subtreeSize}), " +
                            "upper = ($upperBound, ${upperResult.subtreeSize})"
                }
                val type = when {
                    lowerResult.type == null && upperResult.type == null -> null
                    lowerResult.forWarnings || upperResult.forWarnings -> {
                        // Both use the same qualifiers, so forWarnings in one result implies forWarnings (or no changes) in the other.
                        val enhancement = upperResult.type?.let { KotlinTypeFactory.flexibleType(lowerResult.type ?: it, it) }
                            ?: lowerResult.type!!
                        wrapEnhancement(enhancement)
                    }
                    isRawType -> RawTypeImpl(lowerResult.type ?: lowerBound, upperResult.type ?: upperBound)
                    else -> KotlinTypeFactory.flexibleType(lowerResult.type ?: lowerBound, upperResult.type ?: upperBound)
                }
                Result(type, lowerResult.subtreeSize)
            }
            is SimpleType -> {
                val result = enhanceInflexible(
                    qualifiers, index, TypeComponentPosition.INFLEXIBLE, isSuperTypesEnhancement = isSuperTypesEnhancement
                )
                Result(if (result.forWarnings) wrapEnhancement(result.type) else result.type, result.subtreeSize)
            }
        }
    }

    private fun SimpleType.enhanceInflexible(
        qualifiers: (Int) -> JavaTypeQualifiers,
        index: Int,
        position: TypeComponentPosition,
        isBoundOfRawType: Boolean = false,
        isSuperTypesEnhancement: Boolean = false
    ): SimpleResult {
        val shouldEnhance = position.shouldEnhance()
        if (!shouldEnhance && arguments.isEmpty()) return SimpleResult(null, 1, false)

        val originalClass = constructor.declarationDescriptor
            ?: return SimpleResult(null, 1, false)

        val effectiveQualifiers = qualifiers(index)
        val (enhancedClassifier, enhancedMutabilityAnnotations) = originalClass.enhanceMutability(effectiveQualifiers, position)

        val typeConstructor = enhancedClassifier.typeConstructor

        var globalArgIndex = index + 1
        var wereChanges = enhancedMutabilityAnnotations != null
        val enhancedArguments = if (!isSuperTypesEnhancement || !isBoundOfRawType) {
            arguments.mapIndexed { localArgIndex, arg ->
                if (arg.isStarProjection) {
                    val qualifiersForStarProjection = qualifiers(globalArgIndex)
                    globalArgIndex++

                    if (qualifiersForStarProjection.nullability == NOT_NULL && !isBoundOfRawType) {
                        val enhanced = arg.type.unwrap().makeNotNullable()
                        createProjection(enhanced, arg.projectionKind, typeParameterDescriptor = typeConstructor.parameters[localArgIndex])
                    } else {
                        TypeUtils.makeStarProjection(enhancedClassifier.typeConstructor.parameters[localArgIndex])
                    }
                } else {
                    val unwrapped = arg.type.unwrap()
                val enhanced = unwrapped.enhancePossiblyFlexible(qualifiers, globalArgIndex, isSuperTypesEnhancement)
                    globalArgIndex += enhanced.subtreeSize
                    val type = enhanced.type?.also { wereChanges = true } ?: unwrapped
                createProjection(type, arg.projectionKind, typeParameterDescriptor = typeConstructor.parameters[localArgIndex])
                }
            }
        } else {
            globalArgIndex += arguments.size
            arguments
        }

        val (enhancedNullability, enhancedNullabilityAnnotations) = this.getEnhancedNullability(effectiveQualifiers, position)
        wereChanges = wereChanges || enhancedNullabilityAnnotations != null

        val subtreeSize = globalArgIndex - index
        if (!wereChanges) return SimpleResult(null, subtreeSize, false)

        val newAnnotations = listOfNotNull(
            annotations,
            enhancedMutabilityAnnotations,
            enhancedNullabilityAnnotations
        ).compositeAnnotationsOrSingle()

        val enhancedType = KotlinTypeFactory.simpleType(
            newAnnotations,
            typeConstructor,
            enhancedArguments,
            enhancedNullability
        )

        val enhancement = if (effectiveQualifiers.isNotNullTypeParameter) notNullTypeParameter(enhancedType) else enhancedType
        val nullabilityForWarning = enhancedNullabilityAnnotations != null && effectiveQualifiers.isNullabilityQualifierForWarning
        return SimpleResult(enhancement, subtreeSize, nullabilityForWarning)
    }

    private fun notNullTypeParameter(enhancedType: SimpleType) =
        if (javaResolverSettings.correctNullabilityForNotNullTypeParameter)
            enhancedType.makeSimpleTypeDefinitelyNotNullOrNotNull(useCorrectedNullabilityForTypeParameters = true)
        else
            NotNullTypeParameter(enhancedType)
}

private fun List<Annotations>.compositeAnnotationsOrSingle() = when (size) {
    0 -> error("At least one Annotations object expected")
    1 -> single()
    else -> CompositeAnnotations(this.toList())
}

private data class EnhancementResult<out T>(val result: T, val enhancementAnnotations: Annotations?)

private fun <T> T.noChange() = EnhancementResult(this, null)
private fun <T> T.enhancedNullability() = EnhancementResult(this, ENHANCED_NULLABILITY_ANNOTATIONS)
private fun <T> T.enhancedMutability() = EnhancementResult(this, ENHANCED_MUTABILITY_ANNOTATIONS)

private fun ClassifierDescriptor.enhanceMutability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): EnhancementResult<ClassifierDescriptor> {
    if (!position.shouldEnhance()) return this.noChange()
    if (this !is ClassDescriptor) return this.noChange() // mutability is not applicable for type parameters

    val mapper = JavaToKotlinClassMapper

    when (qualifiers.mutability) {
        READ_ONLY -> {
            if (position == TypeComponentPosition.FLEXIBLE_LOWER && mapper.isMutable(this)) {
                return mapper.convertMutableToReadOnly(this).enhancedMutability()
            }
        }
        MUTABLE -> {
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mapper.isReadOnly(this)) {
                return mapper.convertReadOnlyToMutable(this).enhancedMutability()
            }
        }
        null -> {}
    }

    return this.noChange()
}

private fun KotlinType.getEnhancedNullability(qualifiers: JavaTypeQualifiers, position: TypeComponentPosition): EnhancementResult<Boolean> {
    if (!position.shouldEnhance()) return this.isMarkedNullable.noChange()

    return when (qualifiers.nullability) {
        NULLABLE -> true.enhancedNullability()
        NOT_NULL -> false.enhancedNullability()
        else -> this.isMarkedNullable.noChange()
    }
}

private val ENHANCED_NULLABILITY_ANNOTATIONS = EnhancedTypeAnnotations(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)
private val ENHANCED_MUTABILITY_ANNOTATIONS = EnhancedTypeAnnotations(JvmAnnotationNames.ENHANCED_MUTABILITY_ANNOTATION)

private class EnhancedTypeAnnotations(private val fqNameToMatch: FqName) : Annotations {
    override fun isEmpty() = false

    override fun findAnnotation(fqName: FqName) = when (fqName) {
        fqNameToMatch -> EnhancedTypeAnnotationDescriptor
        else -> null
    }

    // Note, that this class may break Annotations contract (!isEmpty && iterator.isEmpty())
    // It's a hack that we need unless we have stable "user data" in JetType
    override fun iterator(): Iterator<AnnotationDescriptor> = emptyList<AnnotationDescriptor>().iterator()
}

private object EnhancedTypeAnnotationDescriptor : AnnotationDescriptor {
    private fun throwError(): Nothing = error("No methods should be called on this descriptor. Only its presence matters")
    override val type: KotlinType get() = throwError()
    override val allValueArguments: Map<Name, ConstantValue<*>> get() = throwError()
    override val source: SourceElement get() = throwError()
    override fun toString() = "[EnhancedType]"
}

internal class NotNullTypeParameter(override val delegate: SimpleType) : NotNullTypeVariable, DelegatingSimpleType() {

    override val isTypeVariable: Boolean
        get() = true

    override fun substitutionResult(replacement: KotlinType): KotlinType {
        val unwrappedType = replacement.unwrap()
        if (!unwrappedType.isTypeParameter() && !TypeUtils.isNullableType(unwrappedType)) return unwrappedType

        return when (unwrappedType) {
            is SimpleType -> unwrappedType.prepareReplacement()
            is FlexibleType -> KotlinTypeFactory.flexibleType(
                unwrappedType.lowerBound.prepareReplacement(),
                unwrappedType.upperBound.prepareReplacement()
            ).wrapEnhancement(unwrappedType.getEnhancement())
            else -> error("Incorrect type: $unwrappedType")
        }
    }

    override val isMarkedNullable: Boolean
        get() = false

    private fun SimpleType.prepareReplacement(): SimpleType {
        val result = makeNullableAsSpecified(false)
        if (!this.isTypeParameter()) return result

        return NotNullTypeParameter(result)
    }

    override fun replaceAnnotations(newAnnotations: Annotations) = NotNullTypeParameter(delegate.replaceAnnotations(newAnnotations))
    override fun makeNullableAsSpecified(newNullability: Boolean) =
        if (newNullability) delegate.makeNullableAsSpecified(true) else this

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = NotNullTypeParameter(delegate)
}
