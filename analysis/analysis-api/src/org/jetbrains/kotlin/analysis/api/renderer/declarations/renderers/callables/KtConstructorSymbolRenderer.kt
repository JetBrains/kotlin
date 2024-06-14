/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.renderer.render

@KaExperimentalApi
public interface KaConstructorSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaConstructorSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaConstructorSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            declarationRenderer.callableSignatureRenderer
                .renderCallableSignature(analysisSession, symbol, KtTokens.CONSTRUCTOR_KEYWORD, declarationRenderer, printer)

            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, printer)
        }
    }

    public object AS_RAW_SIGNATURE : KaConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaConstructorSymbol,
            declarationRenderer: KaDeclarationRenderer,
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
                            (symbol.containingSymbol as? KaNamedSymbol)?.name?.let { printer.append(it.render()) }
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

@KaExperimentalApi
@Deprecated("Use 'KaConstructorSymbolRenderer' instead", ReplaceWith("KaConstructorSymbolRenderer"))
public typealias KtConstructorSymbolRenderer = KaConstructorSymbolRenderer