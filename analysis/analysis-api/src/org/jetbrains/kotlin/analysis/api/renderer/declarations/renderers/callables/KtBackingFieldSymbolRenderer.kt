/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtBackingFieldSymbolRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtBackingFieldSymbol, printer: PrettyPrinter)

    public object AS_FIELD_KEYWORD : KtBackingFieldSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtBackingFieldSymbol, printer: PrettyPrinter): Unit = printer {
            codeStyle.getSeparatorBetweenAnnotationAndOwner(symbol).separated(
                { annotationRenderer.renderAnnotations(symbol, printer) },
                {
                    keywordsRenderer.renderKeyword(KtTokens.FIELD_KEYWORD, symbol, printer)
                },
            )
        }
    }
}