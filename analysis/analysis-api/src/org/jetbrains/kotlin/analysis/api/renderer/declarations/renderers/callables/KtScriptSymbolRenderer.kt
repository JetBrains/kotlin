/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtScriptSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KtAnalysisSession,
        symbol: KtScriptSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KtScriptSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtScriptSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            declarationRenderer.scriptInitializerRenderer.renderInitializer(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}