/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*

open class ConeClassTypeImpl(
    override val lookupTag: ConeClassLikeLookupTag,
    override val typeArguments: Array<out ConeKotlinTypeProjection>,
    isNullable: Boolean
) : ConeClassType() {
    override val nullability: ConeNullability = ConeNullability.create(isNullable)
}

class ConeAbbreviatedTypeImpl(
    override val abbreviationLookupTag: ConeClassLikeLookupTag,
    override val typeArguments: Array<out ConeKotlinTypeProjection>,
    isNullable: Boolean
) : ConeAbbreviatedType() {
    override val lookupTag: ConeClassLikeLookupTag
        get() = abbreviationLookupTag

    override val nullability: ConeNullability = ConeNullability.create(isNullable)
}

class ConeTypeParameterTypeImpl(
    override val lookupTag: ConeTypeParameterLookupTag,
    isNullable: Boolean
) : ConeTypeParameterType() {
    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = EMPTY_ARRAY

    override val nullability: ConeNullability = ConeNullability.create(isNullable)
}
