/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

class FirIdRendererBasedSymbolRenderer : FirSymbolRenderer() {
    override fun printReference(symbol: FirBasedSymbol<*>) {
        when (symbol) {
            is FirCallableSymbol<*> -> components.idRenderer.renderCallableId(symbol.callableId)
            is FirClassLikeSymbol<*> -> components.idRenderer.renderClassId(symbol.classId)
            else -> super.printReference(symbol)
        }
    }
}