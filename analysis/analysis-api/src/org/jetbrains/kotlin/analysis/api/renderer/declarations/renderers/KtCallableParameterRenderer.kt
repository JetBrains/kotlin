/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaCallableParameterRenderer {
    public fun renderValueParameters(
        analysisSession: KaSession,
        symbol: KaCallableSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object PARAMETERS_IN_PARENS : KaCallableParameterRenderer {
        override fun renderValueParameters(
            analysisSession: KaSession,
            symbol: KaCallableSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            val valueParameters = when (symbol) {
                is KaFunctionLikeSymbol -> symbol.valueParameters
                else -> return
            }
            printer.printCollection(valueParameters, prefix = "(", postfix = ")") {
                declarationRenderer.renderDeclaration(analysisSession, it, printer)
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaCallableParameterRenderer' instead", ReplaceWith("KaCallableParameterRenderer"))
public typealias KtCallableParameterRenderer = KaCallableParameterRenderer