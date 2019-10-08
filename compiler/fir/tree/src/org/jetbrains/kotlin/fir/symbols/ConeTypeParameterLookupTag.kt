/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name

data class ConeTypeParameterLookupTag(val typeParameterSymbol: FirTypeParameterSymbol) : ConeClassifierLookupTagWithFixedSymbol() {
    override val name: Name get() = typeParameterSymbol.name
    override val symbol: FirClassifierSymbol<*>
        get() = typeParameterSymbol

}

