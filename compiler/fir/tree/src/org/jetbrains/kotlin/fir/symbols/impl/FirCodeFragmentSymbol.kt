/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId

class FirCodeFragmentSymbol(symbolId: FirSymbolId<FirCodeFragmentSymbol>) : FirBasedSymbol<FirCodeFragment>(symbolId) {
    /**
     * Creates a [FirCodeFragmentSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())
}
