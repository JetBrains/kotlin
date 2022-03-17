/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.typeUtil.extractTypeParametersFromUpperBounds
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjectionOrMapped
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections

internal class TypeParameterUpperBoundEraser(rawSubstitution: RawSubstitution? = null) {
    private val storage = LockBasedStorageManager("Type parameter upper bound erasion results")
    private val erroneousErasedBound by lazy {
        ErrorUtils.createErrorType(ErrorTypeKind.CANNOT_COMPUTE_ERASED_BOUND, this.toString())
    }
    private val rawSubstitution = rawSubstitution ?: RawSubstitution(this)

    private data class DataToEraseUpperBound(
        val typeParameter: TypeParameterDescriptor,
        val isRaw: Boolean,
        val typeAttr: JavaTypeAttributes
    ) {
        override fun equals(other: Any?): Boolean {
            if (other !is DataToEraseUpperBound) return false
            return other.typeParameter == this.typeParameter
                    && other.isRaw == this.isRaw
                    && other.typeAttr.flexibility == this.typeAttr.flexibility
                    && other.typeAttr.howThisTypeIsUsed == this.typeAttr.howThisTypeIsUsed
                    && other.typeAttr.isForAnnotationParameter == this.typeAttr.isForAnnotationParameter
                    && other.typeAttr.defaultType == this.typeAttr.defaultType
        }

        override fun hashCode(): Int {
            var result = typeParameter.hashCode()
            result += 31 * result + if (isRaw) 1 else 0
            result += 31 * result + typeAttr.flexibility.hashCode()
            result += 31 * result + typeAttr.howThisTypeIsUsed.hashCode()
            result += 31 * result + if (typeAttr.isForAnnotationParameter) 1 else 0
            result += 31 * result + typeAttr.defaultType.hashCode()
            return result
        }
    }

    private val getErasedUpperBound = storage.createMemoizedFunction<DataToEraseUpperBound, KotlinType> {
        with(it) { getErasedUpperBoundInternal(typeParameter, isRaw, typeAttr) }
    }

    internal fun getErasedUpperBound(
        typeParameter: TypeParameterDescriptor,
        isRaw: Boolean,
        typeAttr: JavaTypeAttributes
    ) = getErasedUpperBound(DataToEraseUpperBound(typeParameter, isRaw, typeAttr))

    private fun getDefaultType(typeAttr: JavaTypeAttributes) =
        typeAttr.defaultType?.replaceArgumentsWithStarProjections() ?: erroneousErasedBound

    // Definition:
    // ErasedUpperBound(T : G<t>) = G<*> // UpperBound(T) is a type G<t> with arguments
    // ErasedUpperBound(T : A) = A // UpperBound(T) is a type A without arguments
    // ErasedUpperBound(T : F) = UpperBound(F) // UB(T) is another type parameter F
    private fun getErasedUpperBoundInternal(
        // Calculation of `potentiallyRecursiveTypeParameter.upperBounds` may recursively depend on `this.getErasedUpperBound`
        // E.g. `class A<T extends A, F extends A>`
        // To prevent recursive calls return defaultValue() instead
        typeParameter: TypeParameterDescriptor,
        isRaw: Boolean,
        typeAttr: JavaTypeAttributes
    ): KotlinType {
        val visitedTypeParameters = typeAttr.visitedTypeParameters

        if (visitedTypeParameters != null && typeParameter.original in visitedTypeParameters)
            return getDefaultType(typeAttr)

        /*
         * We should do erasure of containing type parameters with their erasure to avoid creating inconsistent types.
         * E.g. for `class Foo<T: Foo<B>, B>`, we'd have erasure for lower bound: Foo<Foo<*>, Any>,
         * but it's wrong type: projection(*) != projection(Any).
         * So we should substitute erasure of the corresponding type parameter: `Foo<Foo<Any>, Any>` or `Foo<Foo<*>, *>`.
         */
        val erasedUpperBounds = typeParameter.defaultType.extractTypeParametersFromUpperBounds(visitedTypeParameters).associate {
            val boundProjection = if (visitedTypeParameters == null || it !in visitedTypeParameters) {
                rawSubstitution.computeProjection(
                    it,
                    // if erasure happens due to invalid arguments number, use star projections instead
                    if (isRaw) typeAttr else typeAttr.withFlexibility(JavaTypeFlexibility.INFLEXIBLE),
                    getErasedUpperBound(it, isRaw, typeAttr.withNewVisitedTypeParameter(typeParameter))
                )
            } else makeStarProjection(it, typeAttr)

            it.typeConstructor to boundProjection
        }
        val erasedUpperBoundsSubstitutor = TypeSubstitutor.create(TypeConstructorSubstitution.createByConstructorsMap(erasedUpperBounds))

        val firstUpperBound = typeParameter.upperBounds.first()

        if (firstUpperBound.constructor.declarationDescriptor is ClassDescriptor) {
            return firstUpperBound.replaceArgumentsWithStarProjectionOrMapped(
                erasedUpperBoundsSubstitutor,
                erasedUpperBounds,
                Variance.OUT_VARIANCE,
                typeAttr.visitedTypeParameters
            )
        }

        val stopAt = typeAttr.visitedTypeParameters ?: setOf(this)
        var current = firstUpperBound.constructor.declarationDescriptor as TypeParameterDescriptor

        while (current !in stopAt) {
            val nextUpperBound = current.upperBounds.first()
            if (nextUpperBound.constructor.declarationDescriptor is ClassDescriptor) {
                return nextUpperBound.replaceArgumentsWithStarProjectionOrMapped(
                    erasedUpperBoundsSubstitutor,
                    erasedUpperBounds,
                    Variance.OUT_VARIANCE,
                    typeAttr.visitedTypeParameters
                )
            }

            current = nextUpperBound.constructor.declarationDescriptor as TypeParameterDescriptor
        }

        return getDefaultType(typeAttr)
    }
}