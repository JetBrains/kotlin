/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.types.model.TypeConstructorMarker

sealed class ConeIntegerLiteralType(
    val isUnsigned: Boolean,
    final override val nullability: ConeNullability
) : ConeSimpleKotlinType(), TypeConstructorMarker {
    abstract val possibleTypes: Collection<ConeClassLikeType>
    abstract val supertypes: List<ConeClassLikeType>

    final override val typeArguments: Array<out ConeTypeProjection> get() = EMPTY_ARRAY
    final override val attributes: ConeAttributes get() = ConeAttributes.Empty

    abstract fun getApproximatedType(expectedType: ConeKotlinType? = null): ConeClassLikeType

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeIntegerLiteralType

        if (isUnsigned != other.isUnsigned) return false
        if (possibleTypes != other.possibleTypes) return false
        if (nullability != other.nullability) return false

        return true
    }

    final override fun hashCode(): Int {
        return 31 * possibleTypes.hashCode() + nullability.hashCode()
    }

    companion object
}

abstract class ConeIntegerLiteralConstantType(
    val value: Long,
    isUnsigned: Boolean,
    nullability: ConeNullability
) : ConeIntegerLiteralType(isUnsigned, nullability)

abstract class ConeIntegerConstantOperatorType(
    isUnsigned: Boolean,
    nullability: ConeNullability
) : ConeIntegerLiteralType(isUnsigned, nullability)
