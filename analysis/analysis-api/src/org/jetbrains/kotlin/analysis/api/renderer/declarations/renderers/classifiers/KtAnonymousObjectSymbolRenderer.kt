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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtAnonymousObjectSymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtAnonymousObjectSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtAnonymousObjectSymbol, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                {
                    " : ".separated(
                        { renderAnnotationsModifiersAndContextReceivers(symbol, printer, KtTokens.OBJECT_KEYWORD) },
                        { superTypeListRenderer.renderSuperTypes(symbol, printer) }
                    )
                },
                { classifierBodyRenderer.renderBody(symbol, printer) },
            )
        }
    }
}
