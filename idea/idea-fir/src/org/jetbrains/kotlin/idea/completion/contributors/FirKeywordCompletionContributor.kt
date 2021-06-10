/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.completion.context.*
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.context.FirRawPositionCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirTypeConstraintNameInWhereClausePositionContext
import org.jetbrains.kotlin.idea.completion.context.FirUnknownPositionContext
import org.jetbrains.kotlin.idea.completion.contributors.keywords.OverrideKeywordHandler
import org.jetbrains.kotlin.idea.completion.contributors.keywords.ReturnKeywordHandler
import org.jetbrains.kotlin.idea.completion.contributors.keywords.SuperKeywordHandler
import org.jetbrains.kotlin.idea.completion.contributors.keywords.ThisKeywordHandler
import org.jetbrains.kotlin.idea.completion.keywords.CompletionKeywordHandlerProvider
import org.jetbrains.kotlin.idea.completion.keywords.CompletionKeywordHandlers
import org.jetbrains.kotlin.idea.completion.keywords.DefaultCompletionKeywordHandlerProvider
import org.jetbrains.kotlin.idea.completion.keywords.createLookups
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*

internal class FirKeywordCompletionContributor(basicContext: FirBasicCompletionContext, priority: Int) :
    FirCompletionContributorBase<FirRawPositionCompletionContext>(basicContext, priority) {
    private val keywordCompletion = KeywordCompletion(object : KeywordCompletion.LanguageVersionSettingProvider {
        override fun getLanguageVersionSetting(element: PsiElement) = element.languageVersionSettings
        override fun getLanguageVersionSetting(module: Module) = module.languageVersionSettings
    })

    private val resolveDependentCompletionKeywordHandlers = ResolveDependentCompletionKeywordHandlerProvider(basicContext)

    override fun KtAnalysisSession.complete(positionContext: FirRawPositionCompletionContext) {
        val expression = when (positionContext) {
            is FirNameReferencePositionContext -> {
                val reference = positionContext.reference
                when (reference.expression) {
                    is KtLabelReferenceExpression -> reference.expression.parent.parent as? KtExpressionWithLabel
                    else -> reference.expression
                }
            }
            is FirTypeConstraintNameInWhereClausePositionContext, is FirIncorrectPositionContext, is FirClassifierNamePositionContext -> {
                error("keyword completion should not be called for ${positionContext::class.simpleName}")
            }
            is FirUnknownPositionContext -> null
        }
        completeWithResolve(expression ?: positionContext.position, expression)
    }


    fun KtAnalysisSession.completeWithResolve(position: PsiElement, expression: KtExpression?) {
        complete(position) { lookupElement, keyword ->
            val lookups = DefaultCompletionKeywordHandlerProvider.getHandlerForKeyword(keyword)
                ?.createLookups(parameters, expression, lookupElement, project)
                ?: resolveDependentCompletionKeywordHandlers.getHandlerForKeyword(keyword)?.run {
                    createLookups(parameters, expression, lookupElement, project)
                }
                ?: listOf(lookupElement)
            sink.addAllElements(lookups)
        }
    }

    private inline fun complete(position: PsiElement, crossinline complete: (LookupElement, String) -> Unit) {
        keywordCompletion.complete(position, prefixMatcher, targetPlatform.isJvm()) { lookupElement ->
            val keyword = lookupElement.lookupString
            complete(lookupElement, keyword)
        }
    }
}

private class ResolveDependentCompletionKeywordHandlerProvider(
    basicContext: FirBasicCompletionContext
) : CompletionKeywordHandlerProvider<KtAnalysisSession>() {
    override val handlers = CompletionKeywordHandlers(
        ReturnKeywordHandler,
        OverrideKeywordHandler(basicContext),
        ThisKeywordHandler(basicContext),
        SuperKeywordHandler,
    )
}