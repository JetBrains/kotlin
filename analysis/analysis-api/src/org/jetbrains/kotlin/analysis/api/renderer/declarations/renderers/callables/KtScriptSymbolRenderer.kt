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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtScriptSymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtScriptSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtScriptSymbol, printer: PrettyPrinter) {
            scriptInitializerRenderer.renderInitializer(symbol, printer)
        }
    }
}