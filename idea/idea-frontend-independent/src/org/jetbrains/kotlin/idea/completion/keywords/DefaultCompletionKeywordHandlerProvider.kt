/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.keywords

import com.intellij.codeInsight.completion.CompletionParameters
import org.jetbrains.kotlin.idea.completion.breakOrContinueExpressionItems
import org.jetbrains.kotlin.idea.completion.handlers.createKeywordConstructLookupElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

@OptIn(ExperimentalStdlibApi::class)
object DefaultCompletionKeywordHandlerProvider : CompletionKeywordHandlerProvider<CompletionKeywordHandler.NO_CONTEXT>() {
    private val BREAK_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.BREAK_KEYWORD) { _, expression, _, _ ->
            if (expression != null) {
                breakOrContinueExpressionItems(expression, KtTokens.BREAK_KEYWORD.value)
            } else emptyList()
        }

    private val CONTINUE_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.CONTINUE_KEYWORD) { _, expression, _, _ ->
            if (expression != null) {
                breakOrContinueExpressionItems(expression, KtTokens.CONTINUE_KEYWORD.value)
            } else emptyList()
        }

    private val CONTRACT_HANDLER = completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.CONTRACT_KEYWORD) { _, _, _, _ ->
        emptyList()
    }

    private val GETTER_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.GET_KEYWORD) { parameters, _, lookupElement, project ->
            buildList {
                add(lookupElement)
                if (!parameters.isUseSiteAnnotationTarget) {
                    add(createKeywordConstructLookupElement(project, KtTokens.GET_KEYWORD.value, "val v:Int get()=caret"))
                    add(
                        createKeywordConstructLookupElement(
                            project,
                            KtTokens.GET_KEYWORD.value,
                            "val v:Int get(){caret}",
                            trimSpacesAroundCaret = true
                        )
                    )
                }
            }
        }

    private val SETTER_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.SET_KEYWORD) { parameters, _, lookupElement, project ->
            buildList {
                add(lookupElement)
                if (!parameters.isUseSiteAnnotationTarget) {
                    add(createKeywordConstructLookupElement(project, KtTokens.SET_KEYWORD.value, "var v:Int set(value)=caret"))
                    add(
                        createKeywordConstructLookupElement(
                            project,
                            KtTokens.SET_KEYWORD.value,
                            "var v:Int set(value){caret}",
                            trimSpacesAroundCaret = true
                        )
                    )
                }
            }
        }

    override val handlers = CompletionKeywordHandlers(
        BREAK_HANDLER, CONTINUE_HANDLER,
        GETTER_HANDLER, SETTER_HANDLER,
        CONTRACT_HANDLER,
    )
}


private val CompletionParameters.isUseSiteAnnotationTarget
    get() = position.prevLeaf()?.node?.elementType == KtTokens.AT