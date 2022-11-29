/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsAndModifiers
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

public interface KtCallableSignatureRender {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderCallableSignature(symbol: KtCallableSymbol, keyword: KtKeywordToken?, printer: PrettyPrinter)

    public object FOR_SOURCE : KtCallableSignatureRender {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderCallableSignature(symbol: KtCallableSymbol, keyword: KtKeywordToken?, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                {
                    if (keyword != null) renderAnnotationsAndModifiers(symbol, printer, keyword)
                    else renderAnnotationsAndModifiers(symbol, printer)
                },
                { typeParametersRenderer.renderTypeParameters(symbol, printer) },
                {
                    val receiverSymbol = symbol.receiverParameter
                    if (receiverSymbol != null) {
                        withSuffix(".") { callableReceiverRenderer.renderReceiver(receiverSymbol, printer) }
                    }

                    if (symbol is KtNamedSymbol) {
                        nameRenderer.renderName(symbol, printer)
                    }
                },
            )
            " ".separated(
                {
                    valueParametersRenderer.renderValueParameters(symbol, printer)
                    withPrefix(": ") { returnTypeRenderer.renderReturnType(symbol, printer) }
                },
                { typeParametersRenderer.renderWhereClause(symbol, printer) },
            )
        }
    }
}
