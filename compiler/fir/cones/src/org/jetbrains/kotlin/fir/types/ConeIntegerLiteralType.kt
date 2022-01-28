/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.types.model.TypeConstructorMarker

abstract class ConeIntegerLiteralType(
    val value: Long,
    val isUnsigned: Boolean,
    override val nullability: ConeNullability
) : ConeSimpleKotlinType(), TypeConstructorMarker {
    abstract val possibleTypes: Collection<ConeClassLikeType>
    abstract val supertypes: List<ConeClassLikeType>

    override val typeArguments: Array<out ConeTypeProjection> = emptyArray()

    abstract fun getApproximatedType(expectedType: ConeKotlinType? = null): ConeClassLikeType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeIntegerLiteralType

        if (possibleTypes != other.possibleTypes) return false
        if (nullability != other.nullability) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * possibleTypes.hashCode() + nullability.hashCode()
    }
}

fun ConeIntegerLiteralType.canBeInt(): Boolean {
    return value in Int.MIN_VALUE..Int.MAX_VALUE
}
