/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtScriptRenderer {

    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtScriptSymbol, printer: PrettyPrinter)


    public object RENDERER : KtScriptRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtScriptSymbol, printer: PrettyPrinter) {
            printer.append("Hren znaet chto eto")
        }
    }
}