/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

object FirKeywordCompletion {
    private val keywordCompletion = KeywordCompletion(object : KeywordCompletion.LanguageVersionSettingProvider {
        override fun getLanguageVersionSetting(element: PsiElement) = LanguageVersionSettingsImpl.DEFAULT // TODO
        override fun getLanguageVersionSetting(module: Module) = LanguageVersionSettingsImpl.DEFAULT // TODO
    })

    fun completeKeywords(result: CompletionResultSet, position: PsiElement, prefixMatcher: PrefixMatcher, isJvmModule: Boolean) {
        keywordCompletion.complete(position, prefixMatcher, isJvmModule) { lookupElement ->
            result.addElement(lookupElement)
        }
    }
}