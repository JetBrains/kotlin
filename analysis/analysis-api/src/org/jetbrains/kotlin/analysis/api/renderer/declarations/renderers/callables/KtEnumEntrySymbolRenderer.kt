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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtEnumEntrySymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtEnumEntrySymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtEnumEntrySymbol, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                { renderAnnotationsModifiersAndContextReceivers(symbol, printer) },
                { nameRenderer.renderName(symbol, printer) },
                { symbol.enumEntryInitializer?.let { classifierBodyRenderer.renderBody(it, printer) } },
            )
        }
    }
}