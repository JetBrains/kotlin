/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaScriptSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaScriptSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaScriptSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaScriptSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            declarationRenderer.scriptInitializerRenderer.renderInitializer(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaScriptSymbolRenderer' instead", ReplaceWith("KaScriptSymbolRenderer"))
public typealias KtScriptSymbolRenderer = KaScriptSymbolRenderer