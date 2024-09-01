/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaAnonymousFunctionSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaAnonymousFunctionSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaAnonymousFunctionSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaAnonymousFunctionSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                declarationRenderer.callableSignatureRenderer
                    .renderCallableSignature(analysisSession, symbol, KtTokens.FUN_KEYWORD, declarationRenderer, printer)
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaAnonymousFunctionSymbolRenderer' instead", ReplaceWith("KaAnonymousFunctionSymbolRenderer"))
public typealias KtAnonymousFunctionSymbolRenderer = KaAnonymousFunctionSymbolRenderer