/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtSamConstructorSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KtAnalysisSession,
        symbol: KtSamConstructorSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object NOT_RENDER : KtSamConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtSamConstructorSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {}
        }
    }

    public object AS_FUNCTION : KtSamConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtSamConstructorSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                declarationRenderer.callableSignatureRenderer
                    .renderCallableSignature(analysisSession, symbol, keyword = null, declarationRenderer, printer)
            }
        }
    }
}