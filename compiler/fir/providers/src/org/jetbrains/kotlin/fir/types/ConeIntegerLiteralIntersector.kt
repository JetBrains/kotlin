/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

object ConeIntegerLiteralIntersector {
    fun findCommonIntersectionType(types: Collection<ConeKotlinType>): ConeSimpleKotlinType? {
        val ilt = types.find { it is ConeIntegerLiteralType } ?: return null
        return types.fold(ilt as ConeSimpleKotlinType) { acc: ConeSimpleKotlinType?, right: ConeKotlinType -> fold(acc, right) }
    }

    private fun fold(left: ConeSimpleKotlinType?, right: ConeKotlinType): ConeSimpleKotlinType? {
        if (left == null) return null
        return when {
            left is ConeIntegerLiteralType && right is ConeIntegerLiteralType ->
                fold(left, right)

            left is ConeIntegerLiteralType -> fold(left, right)
            right is ConeIntegerLiteralType -> fold(right, left)
            else -> null
        }
    }

    private fun fold(left: ConeIntegerLiteralType, right: ConeIntegerLiteralType): ConeIntegerLiteralType? {
        return when {
            left.possibleTypes.containsAll(right.possibleTypes) -> right
            right.possibleTypes.containsAll(left.possibleTypes) -> left
            else -> null
        }
    }

    private fun fold(left: ConeIntegerLiteralType, right: ConeKotlinType): ConeClassLikeType? =
        if (right in left.possibleTypes) (right as ConeClassLikeType) else null
}