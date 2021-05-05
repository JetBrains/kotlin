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

class CompletionKeywordHandlers(handlers: List<CompletionKeywordHandler>) {
    constructor(vararg handlers: CompletionKeywordHandler): this(handlers.toList())

    private val handlerByKeyword = handlers.associateBy { it.keyword.value }

    fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler? =
        handlerByKeyword[keyword]
}

@OptIn(ExperimentalStdlibApi::class)
object DefaultCompletionKeywordHandlers {
    private val BREAK_HANDLER = CompletionKeywordHandler(KtTokens.BREAK_KEYWORD) { _, expression, _, _ ->
        if (expression != null) {
            breakOrContinueExpressionItems(expression, KtTokens.BREAK_KEYWORD.value)
        } else emptyList()
    }

    private val CONTINUE_HANDLER = CompletionKeywordHandler(KtTokens.CONTINUE_KEYWORD) { _, expression, _, _ ->
        if (expression != null) {
            breakOrContinueExpressionItems(expression, KtTokens.CONTINUE_KEYWORD.value)
        } else emptyList()
    }

    private val CONTRACT_HANDLER = CompletionKeywordHandler(KtTokens.CONTRACT_KEYWORD) { _, _, _, _ ->
        emptyList()
    }

    private val GETTER_HANDLER = CompletionKeywordHandler(KtTokens.GET_KEYWORD) { parameters, _, lookupElement, project ->
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

    private val SETTER_HANDLER = CompletionKeywordHandler(KtTokens.SET_KEYWORD) { parameters, _, lookupElement, project ->
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

private val CompletionParameters.isUseSiteAnnotationTarget
    get() = position.prevLeaf()?.node?.elementType == KtTokens.AT

abstract class CompletionKeywordHandler(
    val keyword: KtKeywordToken,
) {
    abstract fun createLookups(
        parameters: CompletionParameters,
        expression: KtExpression?,
        lookup: LookupElement,
        project: Project
    ): Collection<LookupElement>

    companion object {
        inline operator fun invoke(
            keyword: KtKeywordToken,
            crossinline create: (
                parameters: CompletionParameters,
                expression: KtExpression?,
                lookup: LookupElement,
                project: Project
            ) -> Collection<LookupElement>
        ) = object : CompletionKeywordHandler(keyword) {
            override fun createLookups(
                parameters: CompletionParameters,
                expression: KtExpression?,
                lookup: LookupElement,
                project: Project
            ): Collection<LookupElement> = create(parameters, expression, lookup, project)
        }
    }
}
