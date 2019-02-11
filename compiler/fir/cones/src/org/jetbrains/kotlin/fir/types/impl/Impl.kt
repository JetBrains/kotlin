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
    override val typeArguments: Array<ConeKotlinTypeProjection>,
    isNullable: Boolean
) : ConeClassLikeType() {
    override val nullability: ConeNullability = ConeNullability.create(isNullable)
}

class ConeAbbreviatedTypeImpl(
    override val abbreviationSymbol: ConeClassLikeSymbol,
    override val typeArguments: Array<ConeKotlinTypeProjection>,
    override val directExpansion: ConeClassLikeType,
    isNullable: Boolean
) : ConeAbbreviatedType() {
    override val symbol: ConeClassLikeSymbol
        get() = abbreviationSymbol

    override val nullability: ConeNullability = ConeNullability.create(isNullable)
}

class ConeTypeParameterTypeImpl(
    override val symbol: ConeTypeParameterSymbol,
    isNullable: Boolean
) : ConeTypeParameterType() {
    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = EMPTY_ARRAY

    override val nullability: ConeNullability = ConeNullability.create(isNullable)
}