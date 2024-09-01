/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaAnonymousObjectSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaAnonymousObjectSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaAnonymousObjectSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaAnonymousObjectSymbol,
            declarationRenderer: KaDeclarationRenderer,
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

@KaExperimentalApi
@Deprecated("Use 'KaAnonymousObjectSymbolRenderer' instead", ReplaceWith("KaAnonymousObjectSymbolRenderer"))
public typealias KtAnonymousObjectSymbolRenderer = KaAnonymousObjectSymbolRenderer