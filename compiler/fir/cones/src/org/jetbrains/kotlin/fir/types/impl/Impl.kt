/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.util.WeakPair

class ConeClassLikeTypeImpl(
    override val lookupTag: ConeClassLikeLookupTag,
    typeArguments: Array<out ConeTypeProjection>,
    isNullable: Boolean,
    override val attributes: ConeAttributes = ConeAttributes.Empty
) : ConeClassLikeType() {
    override val typeArguments = if (typeArguments.isEmpty()) EMPTY_ARRAY else typeArguments

    override val nullability: ConeNullability = ConeNullability.create(isNullable)

    // Cached expanded type and the relevant session
    var cachedExpandedType: WeakPair<*, ConeClassLikeType>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeClassLikeTypeImpl

        if (lookupTag != other.lookupTag) return false
        if (!typeArguments.contentEquals(other.typeArguments)) return false
        if (nullability != other.nullability) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lookupTag.hashCode()
        result = 31 * result + typeArguments.contentHashCode()
        result = 31 * result + nullability.hashCode()
        return result
    }
}
