/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaJavaFieldSymbolRenderer {
    public fun renderSymbol(
        session: KaSession,
        symbol: KaJavaFieldSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaJavaFieldSymbolRenderer {
        override fun renderSymbol(
            session: KaSession,
            symbol: KaJavaFieldSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val mutabilityKeyword = if (symbol.isVal) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD
                declarationRenderer.callableSignatureRenderer
                    .renderCallableSignature(session, symbol, mutabilityKeyword, declarationRenderer, printer)

                declarationRenderer.variableInitializerRenderer.renderInitializer(session, symbol, printer)
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaJavaFieldSymbolRenderer' instead", ReplaceWith("KaJavaFieldSymbolRenderer"))
public typealias KtJavaFieldSymbolRenderer = KaJavaFieldSymbolRenderer