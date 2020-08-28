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

