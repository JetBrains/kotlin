/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

object FirKeywordCompletion {
    private val keywordCompletion = KeywordCompletion(object : KeywordCompletion.LanguageVersionSettingProvider {
        override fun getLanguageVersionSetting(element: PsiElement) = LanguageVersionSettingsImpl.DEFAULT // TODO
        override fun getLanguageVersionSetting(module: Module) = LanguageVersionSettingsImpl.DEFAULT // TODO
    })

    fun completeKeywords(
        result: CompletionResultSet,
        parameters: CompletionParameters,
        position: PsiElement,
        prefixMatcher: PrefixMatcher,
        project: Project,
        isJvmModule: Boolean
    ) {
        val reference = (position.parent as? KtSimpleNameExpression)?.mainReference
        val expression = if (reference != null) { // logic is copy-n-pasted from CompletionSession.expression
            if (reference.expression is KtLabelReferenceExpression) {
                reference.expression.parent.parent as? KtExpressionWithLabel
            } else {
                reference.expression
            }
        } else null
        keywordCompletion.complete(expression ?: position, prefixMatcher, isJvmModule) { lookupElement ->
            val keyword = lookupElement.lookupString
            val completionKeywordHandler = DefaultCompletionKeywordHandlers.defaultHandlers.getHandlerForKeyword(keyword)
            val lookups = completionKeywordHandler
                ?.createLookups(parameters, expression, lookupElement, project)
                ?: listOf(lookupElement)
            result.addAllElements(lookups)
        }
    }
}