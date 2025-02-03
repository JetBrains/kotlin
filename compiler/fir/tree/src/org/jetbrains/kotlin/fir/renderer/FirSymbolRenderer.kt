/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirValueParameterKind
import org.jetbrains.kotlin.fir.declarations.isLegacyContextReceiver
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

open class FirSymbolRenderer {

    internal lateinit var components: FirRendererComponents
    protected val printer: FirPrinter get() = components.printer

    open fun printReference(symbol: FirBasedSymbol<*>) {
        printer.print(renderReference(symbol))
    }

    protected open fun renderReference(symbol: FirBasedSymbol<*>): String {
        return when (symbol) {
            is FirValueParameterSymbol if (symbol.fir.isLegacyContextReceiver()) ->
                "context of ${renderReference(symbol.fir.containingDeclarationSymbol)}"
            is FirCallableSymbol<*> if symbol.origin == FirDeclarationOrigin.Synthetic.WhenSubject -> $$"$subj$"
            is FirCallableSymbol<*> -> symbol.callableId.toString()
            is FirClassLikeSymbol<*> -> symbol.classId.toString()
            is FirReceiverParameterSymbol -> {
                renderReference(symbol.fir.containingDeclarationSymbol)
            }
            else -> "?"
        }
    }
}
