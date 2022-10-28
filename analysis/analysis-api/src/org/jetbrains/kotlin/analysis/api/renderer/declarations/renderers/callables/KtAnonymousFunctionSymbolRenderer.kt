/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtAnonymousFunctionSymbolRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtAnonymousFunctionSymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtAnonymousFunctionSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtAnonymousFunctionSymbol, printer: PrettyPrinter): Unit = printer {
            callableSignatureRenderer.renderCallableSignature(symbol, KtTokens.FUN_KEYWORD, printer)
        }
    }
}