/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KtAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtAnonymousObjectSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KtAnalysisSession,
        symbol: KtAnonymousObjectSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KtAnonymousObjectSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtAnonymousObjectSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ): Unit = printer {
            " ".separated(
                {
                    " : ".separated(
                        { renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer, KtTokens.OBJECT_KEYWORD) },
                        { declarationRenderer.superTypeListRenderer.renderSuperTypes(analysisSession, symbol, declarationRenderer, printer) }
                    )
                },
                { declarationRenderer.classifierBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, printer) },
            )
        }
    }
}
