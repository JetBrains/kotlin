/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

public interface KtCallableSignatureRenderer {
    public fun renderCallableSignature(
        analysisSession: KtAnalysisSession,
        symbol: KtCallableSymbol,
        keyword: KtKeywordToken?,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object FOR_SOURCE : KtCallableSignatureRenderer {
        override fun renderCallableSignature(
            analysisSession: KtAnalysisSession,
            symbol: KtCallableSymbol,
            keyword: KtKeywordToken?,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ): Unit = printer {
            " ".separated(
                {
                    if (keyword != null) {
                        renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer, keyword)
                    } else {
                        renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer)
                    }
                },
                { declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer) },
                {
                    val receiverSymbol = symbol.receiverParameter
                    if (receiverSymbol != null) {
                        withSuffix(".") {
                            declarationRenderer.callableReceiverRenderer
                                .renderReceiver(analysisSession, receiverSymbol, declarationRenderer, printer)
                        }
                    }

                    if (symbol is KtNamedSymbol) {
                        declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer)
                    }
                },
            )
            " ".separated(
                {
                    declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, symbol, declarationRenderer, printer)
                    withPrefix(": ") {
                        declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
                    }
                },
                { declarationRenderer.typeParametersRenderer.renderWhereClause(analysisSession, symbol, declarationRenderer, printer) },
            )
        }
    }
}
