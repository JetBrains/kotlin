/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol

class FirSymbolRendererWithStaticFlag : FirSymbolRenderer() {
    override fun renderReference(symbol: FirBasedSymbol<*>): String {
        return super.renderReference(symbol).let {
            if (symbol is FirCallableSymbol && symbol !is FirEnumEntrySymbol && symbol.isStatic) "$it*s" else it
        }
    }
}