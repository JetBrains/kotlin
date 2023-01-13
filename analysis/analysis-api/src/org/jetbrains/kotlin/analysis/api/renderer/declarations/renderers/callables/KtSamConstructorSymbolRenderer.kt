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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtSamConstructorSymbol, printer: PrettyPrinter)

    public object NOT_RENDER : KtSamConstructorSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtSamConstructorSymbol, printer: PrettyPrinter): Unit = printer {
        }
    }

    public object AS_FUNCTION : KtSamConstructorSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtSamConstructorSymbol, printer: PrettyPrinter): Unit = printer {
            callableSignatureRenderer.renderCallableSignature(symbol, keyword = null, printer)
        }
    }
}