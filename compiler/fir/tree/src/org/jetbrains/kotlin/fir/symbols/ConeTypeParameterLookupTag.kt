/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.TypeParameterMarker

data class ConeTypeParameterLookupTag(
    val typeParameterSymbol: FirTypeParameterSymbol
) : ConeClassifierLookupTagWithFixedSymbol(), TypeParameterMarker {
    override val name: Name get() = typeParameterSymbol.name
    override val symbol: FirTypeParameterSymbol
        get() = typeParameterSymbol
}

/**
 * Make a transformation from marker interface to cone-based type
 *
 * In K2 frontend context such a transformation is normally safe,
 * as K1-based types and IR-based types cannot occur here.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun TypeParameterMarker.asCone(): ConeTypeParameterLookupTag = this as ConeTypeParameterLookupTag

@Deprecated(message = "This call is redundant, please just drop it", level = DeprecationLevel.ERROR)
fun ConeTypeParameterLookupTag.asCone(): ConeTypeParameterLookupTag = this
