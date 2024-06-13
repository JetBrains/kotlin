/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KaFunctionSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaFunctionSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaFunctionSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaFunctionSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            declarationRenderer.callableSignatureRenderer
                .renderCallableSignature(analysisSession, symbol, KtTokens.FUN_KEYWORD, declarationRenderer, printer)

            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, printer)
        }
    }

    public object AS_RAW_SIGNATURE : KaFunctionSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaFunctionSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val receiverSymbol = symbol.receiverParameter
                if (receiverSymbol != null) {
                    withSuffix(".") {
                        declarationRenderer.callableReceiverRenderer
                            .renderReceiver(analysisSession, receiverSymbol, declarationRenderer, printer)
                    }
                }

                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer)

                printer.printCollection(symbol.valueParameters, prefix = "(", postfix = ")") {
                    declarationRenderer.typeRenderer.renderType(analysisSession, it.returnType, printer)
                }

                withPrefix(": ") {
                    declarationRenderer.returnTypeRenderer
                        .renderReturnType(analysisSession, symbol, declarationRenderer, printer)
                }
            }
        }
    }
}

public typealias KtFunctionSymbolRenderer = KaFunctionSymbolRenderer