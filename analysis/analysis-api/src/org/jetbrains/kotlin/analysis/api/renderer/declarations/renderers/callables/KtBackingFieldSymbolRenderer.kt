/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KaBackingFieldSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaBackingFieldSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_FIELD_KEYWORD : KaBackingFieldSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaBackingFieldSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, symbol).separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer) },
                    {
                        declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, KtTokens.FIELD_KEYWORD, symbol, printer)
                    },
                )
            }
        }
    }
}

public typealias KtBackingFieldSymbolRenderer = KaBackingFieldSymbolRenderer