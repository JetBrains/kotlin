/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

internal class CapturedArguments(val capturedArguments: List<TypeProjection>, private val originalType: KotlinType) {
    fun isSuitableForType(type: KotlinType): Boolean {
        val areArgumentsMatched = type.arguments.withIndex().all { (i, typeArgumentsType) ->
            originalType.arguments.size > i && typeArgumentsType == originalType.arguments[i]
        }

        if (!areArgumentsMatched) return false

        val areConstructorsMatched = originalType.constructor == type.constructor
                || FlexibleTypeBoundsChecker.areTypesMayBeLowerAndUpperBoundsOfSameFlexibleTypeByMutability(originalType, type)

        if (!areConstructorsMatched) return false

        return true
    }
}
