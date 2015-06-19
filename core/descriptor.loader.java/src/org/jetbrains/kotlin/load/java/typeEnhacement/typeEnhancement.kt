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
import org.jetbrains.kotlin.load.java.typeEnhacement.JavaTypeQualifiers
import org.jetbrains.kotlin.load.java.typeEnhacement.MutabilityQualifier.MUTABLE
import org.jetbrains.kotlin.load.java.typeEnhacement.MutabilityQualifier.READ_ONLY
import org.jetbrains.kotlin.load.java.typeEnhacement.NullabilityQualifier.NOT_NULL
import org.jetbrains.kotlin.load.java.typeEnhacement.NullabilityQualifier.NULLABLE
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.types.*

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

data class Result(val type: JetType, val subtreeSize: Int)

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
    val enhancedClassifier = originalClass.enhanceMutability(effectiveQualifiers, position)

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
            TypeProjectionImpl(
                    arg.getProjectionKind(),
                    enhancedType
            )
        }
    }

    val enhancedType = JetTypeImpl(
            getAnnotations(),
            enhancedClassifier.getTypeConstructor(),
            this.getEnhancedNullability(effectiveQualifiers, position),
            enhancedArguments,
            if (enhancedClassifier is ClassDescriptor)
                enhancedClassifier.getMemberScope(enhancedArguments)
            else enhancedClassifier.getDefaultType().getMemberScope()
    )
    return Result(enhancedType, globalArgIndex - index)
}

private fun TypeComponentPosition.shouldEnhance() = this != TypeComponentPosition.INFLEXIBLE

private fun ClassifierDescriptor.enhanceMutability(qualifiers: JavaTypeQualifiers, position: TypeComponentPosition): ClassifierDescriptor {
    if (!position.shouldEnhance()) return this
    if (this !is ClassDescriptor) return this // mutability is not applicable for type parameters

    val mapping = JavaToKotlinClassMap.INSTANCE

    when (qualifiers.mutability) {
        READ_ONLY -> {
            if (position == TypeComponentPosition.FLEXIBLE_LOWER && mapping.isMutable(this)) {
                return mapping.convertMutableToReadOnly(this)
            }
        }
        MUTABLE -> {
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mapping.isReadOnly(this) ) {
                return mapping.convertReadOnlyToMutable(this)
            }
        }
    }

    return this
}

private fun JetType.getEnhancedNullability(qualifiers: JavaTypeQualifiers, position: TypeComponentPosition): Boolean {
    if (!position.shouldEnhance()) return this.isMarkedNullable()

    return when (qualifiers.nullability) {
        NULLABLE -> true
        NOT_NULL -> false
        else -> this.isMarkedNullable()
    }
}
