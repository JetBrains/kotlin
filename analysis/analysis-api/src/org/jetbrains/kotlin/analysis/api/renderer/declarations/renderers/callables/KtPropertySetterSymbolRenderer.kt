/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KaPropertySetterSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaPropertySetterSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaPropertySetterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaPropertySetterSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ): Unit = printer {
            " ".separated(
                {
                    renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer, KtTokens.SET_KEYWORD)
                    declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, printer)
                },
                { declarationRenderer.accessorBodyRenderer.renderBody(analysisSession, symbol, printer) },
            )
        }
    }
}

public typealias KtPropertySetterSymbolRenderer = KaPropertySetterSymbolRenderer