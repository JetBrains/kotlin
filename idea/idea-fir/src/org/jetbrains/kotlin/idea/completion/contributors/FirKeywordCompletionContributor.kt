/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.context.FirPositionCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirUnknownPositionContext
import org.jetbrains.kotlin.idea.completion.contributors.keywords.OverrideKeywordHandler
import org.jetbrains.kotlin.idea.completion.contributors.keywords.ReturnKeywordHandler
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*

internal class FirKeywordCompletionContributor(basicContext: FirBasicCompletionContext) : FirCompletionContributorBase(basicContext) {
    private val keywordCompletion = KeywordCompletion(object : KeywordCompletion.LanguageVersionSettingProvider {
        override fun getLanguageVersionSetting(element: PsiElement) = LanguageVersionSettingsImpl.DEFAULT // TODO
        override fun getLanguageVersionSetting(module: Module) = LanguageVersionSettingsImpl.DEFAULT // TODO
    })

    fun KtAnalysisSession.completeKeywords(
        positionContext: FirPositionCompletionContext
    ) {
        val expression = when (positionContext) {
            is FirNameReferencePositionContext -> {
                val reference = positionContext.reference
                when (reference.expression) {
                    is KtLabelReferenceExpression -> reference.expression.parent.parent as? KtExpressionWithLabel
                    else -> reference.expression
                }
            }
            is FirUnknownPositionContext -> null
        }
        completeWithResolve(expression ?: positionContext.position, expression)
    }

    fun KtAnalysisSession.completeWithResolve(position: PsiElement, expression: KtExpression?) {
        complete(position, expression) { lookupElement, keyword ->
            val lookups = DefaultCompletionKeywordHandlers.defaultHandlers.getHandlerForKeyword(keyword)
                ?.createLookups(parameters, expression, lookupElement, project)
                ?: ResolveDependentCompletionKeywordHandlers.handlers.getHandlerForKeyword(keyword)?.run {
                    createLookups(parameters, expression, lookupElement, project)
                }
                ?: listOf(lookupElement)
            result.addAllElements(lookups)
        }
    }

    fun completeDefaultKeywordsWithoutResolve(position: PsiElement, expression: KtExpression?) {
        complete(position, expression) { lookupElement, keyword ->
            val lookups = DefaultCompletionKeywordHandlers.defaultHandlers.getHandlerForKeyword(keyword)
                ?.createLookups(parameters, expression, lookupElement, project)
                ?: listOf(lookupElement)
            result.addAllElements(lookups)
        }
    }

    private inline fun complete(position: PsiElement, expression: KtExpression?, crossinline complete: (LookupElement, String) -> Unit) {
        keywordCompletion.complete(position, prefixMatcher, targetPlatform.isJvm()) { lookupElement ->
            val keyword = lookupElement.lookupString
            complete(lookupElement, keyword)
        }
    }
}

private object ResolveDependentCompletionKeywordHandlers {
    val handlers = CompletionKeywordHandlers(
        ReturnKeywordHandler,
        OverrideKeywordHandler,
    )
}

