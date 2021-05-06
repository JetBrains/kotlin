/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.completion.handlers.createKeywordConstructLookupElement
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

class CompletionKeywordHandlers<CONTEXT>(handlers: List<CompletionKeywordHandler<CONTEXT>>) {
    constructor(vararg handlers: CompletionKeywordHandler<CONTEXT>) : this(handlers.toList())

    private val handlerByKeyword = handlers.associateBy { it.keyword.value }

    fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler<CONTEXT>? =
        handlerByKeyword[keyword]
}

@OptIn(ExperimentalStdlibApi::class)
object DefaultCompletionKeywordHandlers {
    private val BREAK_HANDLER =
        CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.BREAK_KEYWORD) { _, expression, _, _ ->
            if (expression != null) {
                breakOrContinueExpressionItems(expression, KtTokens.BREAK_KEYWORD.value)
            } else emptyList()
        }

    private val CONTINUE_HANDLER =
        CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.CONTINUE_KEYWORD) { _, expression, _, _ ->
            if (expression != null) {
                breakOrContinueExpressionItems(expression, KtTokens.CONTINUE_KEYWORD.value)
            } else emptyList()
        }

    private val CONTRACT_HANDLER = CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.CONTRACT_KEYWORD) { _, _, _, _ ->
        emptyList()
    }

    private val GETTER_HANDLER =
        CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.GET_KEYWORD) { parameters, _, lookupElement, project ->
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
        CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(KtTokens.SET_KEYWORD) { parameters, _, lookupElement, project ->
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

    val defaultHandlers = CompletionKeywordHandlers(
        BREAK_HANDLER, CONTINUE_HANDLER,
        GETTER_HANDLER, SETTER_HANDLER,
        CONTRACT_HANDLER,
    )
}

fun CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>.createLookups(
    parameters: CompletionParameters,
    expression: KtExpression?,
    lookup: LookupElement,
    project: Project
): Collection<LookupElement> = with(CompletionKeywordHandler.NO_CONTEXT) { createLookups(parameters, expression, lookup, project) }

private val CompletionParameters.isUseSiteAnnotationTarget
    get() = position.prevLeaf()?.node?.elementType == KtTokens.AT


abstract class CompletionKeywordHandler<CONTEXT>(
    val keyword: KtKeywordToken
) {
    object NO_CONTEXT

    abstract fun CONTEXT.createLookups(
        parameters: CompletionParameters,
        expression: KtExpression?,
        lookup: LookupElement,
        project: Project
    ): Collection<LookupElement>

    companion object {
        inline operator fun <CONTEXT> invoke(
            keyword: KtKeywordToken,
            crossinline create: CONTEXT.(
                parameters: CompletionParameters,
                expression: KtExpression?,
                lookup: LookupElement,
                project: Project
            ) -> Collection<LookupElement>
        ) = object : CompletionKeywordHandler<CONTEXT>(keyword) {
            override fun CONTEXT.createLookups(
                parameters: CompletionParameters,
                expression: KtExpression?,
                lookup: LookupElement,
                project: Project
            ): Collection<LookupElement> = create(parameters, expression, lookup, project)
        }
    }
}
