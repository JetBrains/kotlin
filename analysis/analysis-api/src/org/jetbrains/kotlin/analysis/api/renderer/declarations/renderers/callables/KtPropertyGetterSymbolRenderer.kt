/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtPropertyGetterSymbolRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtPropertyGetterSymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtPropertyGetterSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtPropertyGetterSymbol, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                {
                    renderAnnotationsModifiersAndContextReceivers(symbol, printer, KtTokens.GET_KEYWORD)
                    valueParametersRenderer.renderValueParameters(symbol, printer)
                },
                { accessorBodyRenderer.renderBody(symbol, printer) },
            )
        }
    }
}