/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtFunctionSymbolRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtFunctionSymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtFunctionSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtFunctionSymbol, printer: PrettyPrinter) {
            callableSignatureRenderer.renderCallableSignature(symbol, KtTokens.FUN_KEYWORD, printer)
            functionLikeBodyRenderer.renderBody(symbol, printer)
        }
    }

    public object AS_RAW_SIGNATURE : KtFunctionSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtFunctionSymbol, printer: PrettyPrinter) {
            printer {
                val receiverSymbol = symbol.receiverParameter
                if (receiverSymbol != null) {
                    withSuffix(".") { callableReceiverRenderer.renderReceiver(receiverSymbol, printer) }
                }
                nameRenderer.renderName(symbol, printer)
                printer.printCollection(symbol.valueParameters, prefix = "(", postfix = ")") {
                    typeRenderer.renderType(it.returnType, printer)
                }
                withPrefix(": ") { returnTypeRenderer.renderReturnType(symbol, printer) }
            }
        }
    }
}