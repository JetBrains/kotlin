/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol

internal object CompletionShortNamesRenderer {
    fun KtAnalysisSession.renderFunctionParameters(function: KtFunctionSymbol): String {
        val receiver = renderReceiver(function)
        val parameters = function.valueParameters.joinToString(", ", "(", ")") { renderFunctionParameter(it) }
        return receiver + parameters
    }

    fun KtAnalysisSession.renderVariable(function: KtVariableLikeSymbol): String {
        return renderReceiver(function)
    }
    
    private fun KtAnalysisSession.renderReceiver(symbol: KtCallableSymbol): String {
        val receiverType = symbol.receiverType?.type ?: return ""
        return receiverType.render(TYPE_RENDERING_OPTIONS) + "."
    }

    private fun KtAnalysisSession.renderFunctionParameter(param: KtValueParameterSymbol): String =
        "${if (param.isVararg) "vararg " else ""}${param.name.asString()}: ${param.annotatedType.type.render(TYPE_RENDERING_OPTIONS)}"

    val TYPE_RENDERING_OPTIONS = KtTypeRendererOptions.SHORT_NAMES
}