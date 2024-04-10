/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.renderer.render

public interface KtConstructorSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KtAnalysisSession,
        symbol: KtConstructorSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KtConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtConstructorSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            declarationRenderer.callableSignatureRenderer
                .renderCallableSignature(analysisSession, symbol, KtTokens.CONSTRUCTOR_KEYWORD, declarationRenderer, printer)

            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, printer)
        }
    }

    public object AS_RAW_SIGNATURE : KtConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtConstructorSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            with(analysisSession) {
                printer {
                    " ".separated(
                        {
                            declarationRenderer.keywordsRenderer
                                .renderKeyword(analysisSession, KtTokens.CONSTRUCTOR_KEYWORD, symbol, printer)
                        },
                        {
                            (symbol.getContainingSymbol() as? KtNamedSymbol)?.name?.let { printer.append(it.render()) }
                            printer.printCollection(symbol.valueParameters, prefix = "(", postfix = ")") {
                                declarationRenderer.typeRenderer.renderType(analysisSession, it.returnType, printer)
                            }
                        }
                    )
                }
            }
        }
    }
}