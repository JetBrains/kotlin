/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.Name

class FirTypeParameterSymbol : AbstractFirBasedSymbol<FirTypeParameter>(), ConeTypeParameterSymbol, ConeTypeParameterLookupTag,
    ConeClassifierLookupTagWithFixedSymbol {

    override val name: Name
        get() = fir.name

    override val symbol: ConeClassifierSymbol
        get() = this

    override fun toLookupTag(): ConeTypeParameterLookupTag = this

    override fun equals(other: Any?): Boolean {
        return other is FirTypeParameterSymbol && fir == other.fir
    }

    override fun hashCode(): Int {
        return fir.hashCode()
    }
}
