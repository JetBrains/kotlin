/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeParameterUpperBoundEraser
import org.jetbrains.kotlin.types.TypeUtils.makeStarProjection

class RawProjectionComputer : ErasureProjectionComputer() {
    override fun computeProjection(
        parameter: TypeParameterDescriptor,
        typeAttr: ErasureTypeAttributes,
        typeParameterUpperBoundEraser: TypeParameterUpperBoundEraser,
        erasedUpperBound: KotlinType
    ): TypeProjection {
        if (typeAttr !is JavaTypeAttributes) {
            return super.computeProjection(parameter, typeAttr, typeParameterUpperBoundEraser, erasedUpperBound)
        }

        // if erasure happens due to invalid arguments number, use star projections instead
        val newTypeAttr = if (typeAttr.isRaw) typeAttr else typeAttr.withFlexibility(JavaTypeFlexibility.INFLEXIBLE)

        return when (newTypeAttr.flexibility) {
            // Raw(List<T>) => (List<Any?>..List<*>)
            // Raw(Enum<T>) => (Enum<Enum<*>>..Enum<out Enum<*>>)
            // In the last case upper bound is equal to star projection `Enum<*>`,
            // but we want to keep matching tree structure of flexible bounds (at least they should have the same size)
            JavaTypeFlexibility.FLEXIBLE_LOWER_BOUND -> TypeProjectionImpl(
                // T : String -> String
                // in T : String -> String
                // T : Enum<T> -> Enum<*>
                Variance.INVARIANT, erasedUpperBound
            )
            JavaTypeFlexibility.FLEXIBLE_UPPER_BOUND, JavaTypeFlexibility.INFLEXIBLE -> {
                if (!parameter.variance.allowsOutPosition) {
                    // in T -> Comparable<Nothing>
                    TypeProjectionImpl(Variance.INVARIANT, parameter.builtIns.nothingType)
                } else if (erasedUpperBound.constructor.parameters.isNotEmpty()) {
                    // T : Enum<E> -> out Enum<*>
                    TypeProjectionImpl(Variance.OUT_VARIANCE, erasedUpperBound)
                } else {
                    // T : String -> *
                    makeStarProjection(parameter, newTypeAttr)
                }
            }
        }
    }
}