/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtValueParameterSymbol

internal object CompletionShortNamesRenderer {
    fun KtAnalysisSession.renderFunctionParameters(function: KtFunctionSymbol): String =
        function.valueParameters.joinToString(", ", "(", ")") { renderFunctionParameter(it) }

    private fun KtAnalysisSession.renderFunctionParameter(param: KtValueParameterSymbol): String =
        "${if (param.isVararg) "vararg " else ""}${param.name.asString()}: ${param.annotatedType.type.render(TYPE_RENDERING_OPTIONS)}"

    val TYPE_RENDERING_OPTIONS = KtTypeRendererOptions.SHORT_NAMES
}