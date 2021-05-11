/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.keywords

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.psi.KtExpression

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
}

inline fun <CONTEXT> completionKeywordHandler(
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

/**
 * Create a list of [LookupElement] for [CompletionKeywordHandler] which has no context
 *
 * This function is needed to avoid writing `with(CompletionKeywordHandler.NO_CONTEXT) { ... }` to create such lookups
 */
fun CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>.createLookups(
    parameters: CompletionParameters,
    expression: KtExpression?,
    lookup: LookupElement,
    project: Project
): Collection<LookupElement> = with(CompletionKeywordHandler.NO_CONTEXT) { createLookups(parameters, expression, lookup, project) }
