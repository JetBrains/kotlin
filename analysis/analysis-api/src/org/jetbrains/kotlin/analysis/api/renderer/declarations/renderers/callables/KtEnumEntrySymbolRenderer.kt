/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtEnumEntrySymbolRenderer {
    public fun renderSymbol(
        analysisSession: KtAnalysisSession,
        symbol: KtEnumEntrySymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KtEnumEntrySymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtEnumEntrySymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ): Unit = printer {
            " ".separated(
                { renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer) },
                { declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer) },
                { symbol.enumEntryInitializer?.let { declarationRenderer.classifierBodyRenderer.renderBody(analysisSession, it, declarationRenderer, printer) } },
            )
        }
    }
}