/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

public interface KaCallableSignatureRenderer {
    public fun renderCallableSignature(
        analysisSession: KaSession,
        symbol: KaCallableSymbol,
        keyword: KtKeywordToken?,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object FOR_SOURCE : KaCallableSignatureRenderer {
        override fun renderCallableSignature(
            analysisSession: KaSession,
            symbol: KaCallableSymbol,
            keyword: KtKeywordToken?,
            declarationRenderer: KaDeclarationRenderer,
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

                    if (symbol is KaNamedSymbol) {
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

public typealias KtCallableSignatureRenderer = KaCallableSignatureRenderer