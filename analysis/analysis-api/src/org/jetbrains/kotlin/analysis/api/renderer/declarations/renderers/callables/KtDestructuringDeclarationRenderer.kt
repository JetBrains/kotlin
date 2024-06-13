/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KaRendererKeywordFilter
import org.jetbrains.kotlin.analysis.api.symbols.KaDestructuringDeclarationSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KaDestructuringDeclarationRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaDestructuringDeclarationSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_ENTRIES : KaDestructuringDeclarationRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaDestructuringDeclarationSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, symbol).separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer) },
                    {
                        // do not render (val a: Int, val b: Int), render `(a: Int, b: Int)` instead
                        val rendererWithoutValVar = declarationRenderer.with {
                            keywordsRenderer = keywordsRenderer.with {
                                keywordFilter =
                                    keywordFilter and KaRendererKeywordFilter.without(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)
                            }
                        }
                        printCollection(symbol.entries, prefix = "(", postfix = ")") {
                            with(rendererWithoutValVar) {
                                renderDeclaration(analysisSession, it, this@printCollection)
                            }
                        }
                    }
                )

            }
        }
    }
}

public typealias KtDestructuringDeclarationRenderer = KaDestructuringDeclarationRenderer