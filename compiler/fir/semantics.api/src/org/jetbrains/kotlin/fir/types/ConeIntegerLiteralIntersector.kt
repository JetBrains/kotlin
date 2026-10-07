/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

object ConeIntegerLiteralIntersector {
    fun findCommonIntersectionType(types: Collection<ConeKotlinType>): ConeKotlinType? {
        if (types.isEmpty()) return null
        return types.reduce { left: ConeKotlinType?, right: ConeKotlinType? -> fold(left, right) }
    }

    private fun fold(left: ConeKotlinType?, right: ConeKotlinType?): ConeKotlinType? {
        if (left == null || right == null) return null
        return when {
            left is ConeIntegerLiteralType && right is ConeIntegerLiteralType ->
                fold(left, right)

            left is ConeIntegerLiteralType -> fold(left, right)
            right is ConeIntegerLiteralType -> fold(right, left)
            else -> null
        }
    }

    private fun fold(left: ConeIntegerLiteralType, right: ConeIntegerLiteralType): ConeKotlinType? {
        return when {
            left.possibleTypes.containsAll(right.possibleTypes) -> right
            right.possibleTypes.containsAll(left.possibleTypes) -> left
            else -> null
        }
    }

    private fun fold(left: ConeIntegerLiteralType, right: ConeKotlinType): ConeKotlinType? =
        if (right in left.possibleTypes) right else null
}