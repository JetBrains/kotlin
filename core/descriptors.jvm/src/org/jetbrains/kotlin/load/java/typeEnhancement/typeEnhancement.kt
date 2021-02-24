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
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.createProjection
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

fun KotlinType.hasEnhancedNullability(): Boolean =
    SimpleClassicTypeSystemContext.hasEnhancedNullability(this)

class JavaTypeEnhancement(private val javaResolverSettings: JavaResolverSettings) {

    private open class Result(open val type: KotlinType, val subtreeSize: Int, val wereChanges: Boolean) {
        val typeIfChanged: KotlinType? get() = type.takeIf { wereChanges }
    }

    private class SimpleResult(override val type: SimpleType, subtreeSize: Int, wereChanges: Boolean) :
        Result(type, subtreeSize, wereChanges)

    // The index in the lambda is the position of the type component:
    // Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
    // which corresponds to the left-to-right breadth-first walk of the tree representation of the type.
    // For flexible types, both bounds are indexed in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`.
    fun KotlinType.enhance(qualifiers: (Int) -> JavaTypeQualifiers) = unwrap().enhancePossiblyFlexible(qualifiers, 0).typeIfChanged

    private fun buildEnhancementByFlexibleTypeBounds(lowerBound: KotlinType, upperBound: KotlinType): KotlinType? {
        val upperEnhancement = upperBound.getEnhancement()
        val lowerEnhancement = lowerBound.getEnhancement() ?: upperEnhancement ?: return null

        if (upperEnhancement == null) return lowerEnhancement

        return KotlinTypeFactory.flexibleType(lowerEnhancement.lowerIfFlexible(), upperEnhancement.upperIfFlexible())
    }

    private fun UnwrappedType.enhancePossiblyFlexible(qualifiers: (Int) -> JavaTypeQualifiers, index: Int): Result {
        if (isError) return Result(this, 1, false)
        return when (this) {
            is FlexibleType -> {
                val isRawType = this is RawType
                val lowerResult = lowerBound.enhanceInflexible(qualifiers, index, TypeComponentPosition.FLEXIBLE_LOWER, isRawType)
                val upperResult = upperBound.enhanceInflexible(qualifiers, index, TypeComponentPosition.FLEXIBLE_UPPER, isRawType)
                assert(lowerResult.subtreeSize == upperResult.subtreeSize) {
                    "Different tree sizes of bounds: " +
                            "lower = ($lowerBound, ${lowerResult.subtreeSize}), " +
                            "upper = ($upperBound, ${upperResult.subtreeSize})"
                }

                val wereChanges = lowerResult.wereChanges || upperResult.wereChanges
                val enhancement = buildEnhancementByFlexibleTypeBounds(lowerResult.type, upperResult.type)
                val type = if (wereChanges) {
                    when (this) {
                        is RawTypeImpl -> RawTypeImpl(lowerResult.type, upperResult.type)
                        else -> KotlinTypeFactory.flexibleType(lowerResult.type, upperResult.type)
                    }.wrapEnhancement(enhancement)
                } else this@enhancePossiblyFlexible

                Result(
                    type,
                    lowerResult.subtreeSize,
                    wereChanges
                )
            }
            is SimpleType -> enhanceInflexible(qualifiers, index, TypeComponentPosition.INFLEXIBLE)
        }
    }

    private fun SimpleType.enhanceInflexible(
        qualifiers: (Int) -> JavaTypeQualifiers,
        index: Int,
        position: TypeComponentPosition,
        isBoundOfRawType: Boolean = false
    ): SimpleResult {
        val shouldEnhance = position.shouldEnhance()
        if (!shouldEnhance && arguments.isEmpty()) return SimpleResult(this, 1, false)

        val originalClass = constructor.declarationDescriptor
            ?: return SimpleResult(this, 1, false)

        val effectiveQualifiers = qualifiers(index)
        val (enhancedClassifier, enhancedMutabilityAnnotations) = originalClass.enhanceMutability(effectiveQualifiers, position)

        val typeConstructor = enhancedClassifier.typeConstructor

        var globalArgIndex = index + 1
        var wereChanges = enhancedMutabilityAnnotations != null
        val enhancedArguments = arguments.mapIndexed { localArgIndex, arg ->
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
                val enhanced = arg.type.unwrap().enhancePossiblyFlexible(qualifiers, globalArgIndex)
                wereChanges = wereChanges || enhanced.wereChanges
                globalArgIndex += enhanced.subtreeSize
                createProjection(enhanced.type, arg.projectionKind, typeParameterDescriptor = typeConstructor.parameters[localArgIndex])
            }
        }

        val (enhancedNullability, enhancedNullabilityAnnotations) = this.getEnhancedNullability(effectiveQualifiers, position)
        wereChanges = wereChanges || enhancedNullabilityAnnotations != null

        val subtreeSize = globalArgIndex - index
        if (!wereChanges) return SimpleResult(this, subtreeSize, wereChanges = false)

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
        val result = if (nullabilityForWarning) wrapEnhancement(enhancement) else enhancement

        return SimpleResult(result as SimpleType, subtreeSize, wereChanges = true)
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
