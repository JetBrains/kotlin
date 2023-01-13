/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtCallableParameterRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderValueParameters(symbol: KtCallableSymbol, printer: PrettyPrinter)

    public object PARAMETERS_IN_PARENS : KtCallableParameterRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderValueParameters(symbol: KtCallableSymbol, printer: PrettyPrinter) {
            val valueParameters = when (symbol) {
                is KtFunctionLikeSymbol -> symbol.valueParameters
                else -> return
            }
            printer.printCollection(valueParameters, prefix = "(", postfix = ")") {
                renderDeclaration(it, printer)
            }
        }
    }
}

