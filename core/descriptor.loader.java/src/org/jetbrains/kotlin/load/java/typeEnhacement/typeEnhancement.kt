/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.typeEnhacement

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.typeEnhacement.MutabilityQualifier.MUTABLE
import org.jetbrains.kotlin.load.java.typeEnhacement.MutabilityQualifier.READ_ONLY
import org.jetbrains.kotlin.load.java.typeEnhacement.NullabilityQualifier.NOT_NULL
import org.jetbrains.kotlin.load.java.typeEnhacement.NullabilityQualifier.NULLABLE
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.createProjection

// The index in the lambda is the position of the type component:
// Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
// which corresponds to the left-to-right breadth-first walk of the tree representation of the type.
// For flexible types, both bounds are indexed in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`.
fun JetType.enhance(qualifiers: (Int) -> JavaTypeQualifiers) = this.enhancePossiblyFlexible(qualifiers, 0).type


private enum class TypeComponentPosition {
    FLEXIBLE_LOWER,
    FLEXIBLE_UPPER,
    INFLEXIBLE
}

private data class Result(val type: JetType, val subtreeSize: Int)

private fun JetType.enhancePossiblyFlexible(qualifiers: (Int) -> JavaTypeQualifiers, index: Int): Result {
    if (this.isError()) return Result(this, 1)
    return if (this.isFlexible()) {
        with(this.flexibility()) {
            val lowerResult = lowerBound.enhanceInflexible(qualifiers, index, TypeComponentPosition.FLEXIBLE_LOWER)
            val upperResult = upperBound.enhanceInflexible(qualifiers, index, TypeComponentPosition.FLEXIBLE_UPPER)
            assert(lowerResult.subtreeSize == upperResult.subtreeSize) {
                "Different tree sizes of bounds: " +
                "lower = ($lowerBound, ${lowerResult.subtreeSize}), " +
                "upper = ($upperBound, ${upperResult.subtreeSize})"
            }
            Result(
                DelegatingFlexibleType.create(lowerResult.type, upperResult.type, extraCapabilities), lowerResult.subtreeSize
            )
        }
    }
    else this.enhanceInflexible(qualifiers, index, TypeComponentPosition.INFLEXIBLE)
}

private fun JetType.enhanceInflexible(qualifiers: (Int) -> JavaTypeQualifiers, index: Int, position: TypeComponentPosition): Result {
    val shouldEnhance = position.shouldEnhance()
    if (!shouldEnhance && getArguments().isEmpty()) return Result(this, 1)

    val originalClass = getConstructor().getDeclarationDescriptor()
                        ?: return Result(this, 1)

    val effectiveQualifiers = qualifiers(index)
    val (enhancedClassifier, enhancedMutabilityAnnotations) = originalClass.enhanceMutability(effectiveQualifiers, position)

    val typeConstructor = enhancedClassifier.typeConstructor

    var globalArgIndex = index + 1
    val enhancedArguments = getArguments().mapIndexed {
        localArgIndex, arg ->
        if (arg.isStarProjection()) {
            globalArgIndex++
            TypeUtils.makeStarProjection(enhancedClassifier.getTypeConstructor().getParameters()[localArgIndex])
        }
        else {
            val (enhancedType, subtreeSize) = arg.getType().enhancePossiblyFlexible(qualifiers, globalArgIndex)
            globalArgIndex += subtreeSize
            createProjection(enhancedType, arg.projectionKind, typeParameterDescriptor = typeConstructor.parameters[localArgIndex])
        }
    }

    val (enhancedNullability, enhancedNullabilityAnnotations) = this.getEnhancedNullability(effectiveQualifiers, position)
    val newAnnotations = listOf(
            getAnnotations(),
            enhancedMutabilityAnnotations,
            enhancedNullabilityAnnotations
    ).filterNotNull().compositeAnnotationsOrSingle()

    val newSubstitution = computeNewSubstitution(
        typeConstructor.parameters, enhancedArguments
    )

    val enhancedType = JetTypeImpl.create(
            newAnnotations,
            typeConstructor,
            enhancedNullability,
            enhancedArguments,
            newSubstitution,
            if (enhancedClassifier is ClassDescriptor)
                enhancedClassifier.getMemberScope(newSubstitution)
            else enhancedClassifier.getDefaultType().getMemberScope(),
            capabilities
    )
    return Result(enhancedType, globalArgIndex - index)
}

private fun List<Annotations>.compositeAnnotationsOrSingle() = when (size()) {
    0 -> error("At least one Annotations object expected")
    1 -> single()
    else -> CompositeAnnotations(this)
}

private fun TypeComponentPosition.shouldEnhance() = this != TypeComponentPosition.INFLEXIBLE

private data class EnhancementResult<T>(val result: T, val enhancementAnnotations: Annotations?)
private fun <T> T.noChange() = EnhancementResult(this, null)
private fun <T> T.enhancedNullability() = EnhancementResult(this, ENHANCED_NULLABILITY_ANNOTATIONS)
private fun <T> T.enhancedMutability() = EnhancementResult(this, ENHANCED_MUTABILITY_ANNOTATIONS)

private fun ClassifierDescriptor.enhanceMutability(qualifiers: JavaTypeQualifiers, position: TypeComponentPosition): EnhancementResult<ClassifierDescriptor> {
    if (!position.shouldEnhance()) return this.noChange()
    if (this !is ClassDescriptor) return this.noChange() // mutability is not applicable for type parameters

    val mapping = JavaToKotlinClassMap.INSTANCE

    when (qualifiers.mutability) {
        READ_ONLY -> {
            if (position == TypeComponentPosition.FLEXIBLE_LOWER && mapping.isMutable(this)) {
                return mapping.convertMutableToReadOnly(this).enhancedMutability()
            }
        }
        MUTABLE -> {
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mapping.isReadOnly(this) ) {
                return mapping.convertReadOnlyToMutable(this).enhancedMutability()
            }
        }
    }

    return this.noChange()
}

private fun JetType.getEnhancedNullability(qualifiers: JavaTypeQualifiers, position: TypeComponentPosition): EnhancementResult<Boolean> {
    if (!position.shouldEnhance()) return this.isMarkedNullable().noChange()

    return when (qualifiers.nullability) {
        NULLABLE -> true.enhancedNullability()
        NOT_NULL -> false.enhancedNullability()
        else -> this.isMarkedNullable().noChange()
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

    override fun findExternalAnnotation(fqName: FqName) = null

    // Note, that this class may break Annotations contract (!isEmpty && iterator.isEmpty())
    // It's a hack that we need unless we have stable "user data" in JetType
    override fun iterator(): Iterator<AnnotationDescriptor> = emptyList<AnnotationDescriptor>().iterator()
}

private object EnhancedTypeAnnotationDescriptor : AnnotationDescriptor {
    private fun throwError() = error("No methods should be called on this descriptor. Only its presence matters")
    override fun getType() = throwError()
    override fun getAllValueArguments() = throwError()
    override fun getSource() = throwError()
    override fun toString() = "[EnhancedType]"
}
