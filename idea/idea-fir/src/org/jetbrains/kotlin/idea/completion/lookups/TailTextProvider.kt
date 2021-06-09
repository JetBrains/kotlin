/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups

import org.jetbrains.kotlin.idea.completion.KotlinIdeaCompletionBundle
import org.jetbrains.kotlin.idea.completion.lookups.CompletionShortNamesRenderer.renderFunctionParameters
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtFunctionalType

internal object TailTextProvider {
    fun KtAnalysisSession.getTailText(symbol: KtCallableSymbol): String = buildString {
        if (symbol is KtFunctionSymbol) {
            if (insertLambdaBraces(symbol)) {
                append(" {...}")
            } else {
                append(renderFunctionParameters(symbol))
            }
        }
        symbol.receiverType?.type?.let{ receiverType ->
            val renderedType = receiverType.render(CompletionShortNamesRenderer.TYPE_RENDERING_OPTIONS)
            append(KotlinIdeaCompletionBundle.message("presentation.tail.for.0", renderedType))
        }
    }

    fun KtAnalysisSession.insertLambdaBraces(symbol: KtFunctionSymbol): Boolean {
        val singleParam = symbol.valueParameters.singleOrNull()
        return singleParam != null && !singleParam.hasDefaultValue && singleParam.annotatedType.type is KtFunctionalType
    }

    fun KtAnalysisSession.insertLambdaBraces(symbol: KtFunctionalType): Boolean {
        val singleParam = symbol.parameterTypes.singleOrNull()
        return singleParam != null && singleParam is KtFunctionalType
    }
}