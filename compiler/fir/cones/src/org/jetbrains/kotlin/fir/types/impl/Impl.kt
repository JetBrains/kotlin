/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

open class ConeClassTypeImpl(
    override val symbol: ConeClassLikeSymbol,
    override val typeArguments: List<ConeKotlinTypeProjection>
) : ConeClassLikeType()

class ConeKotlinTypeProjectionInImpl(override val type: ConeKotlinType) : ConeKotlinTypeProjectionIn()

class ConeKotlinTypeProjectionOutImpl(override val type: ConeKotlinType) : ConeKotlinTypeProjectionOut()

class ConeKotlinErrorType(val reason: String) : ConeKotlinType() {

    override fun toString(): String {
        return "<ERROR TYPE: $reason>"
    }
}

class ConeAbbreviatedTypeImpl(
    override val abbreviationSymbol: ConeClassLikeSymbol,
    override val typeArguments: List<ConeKotlinTypeProjection>,
    override val directExpansion: ConeClassLikeType
) : ConeAbbreviatedType() {
    override val symbol: ConeClassLikeSymbol
        get() = abbreviationSymbol
}

class ConeTypeParameterTypeImpl(override val symbol: ConeTypeParameterSymbol) : ConeTypeParameterType()