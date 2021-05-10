/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.context

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.completion.KotlinFirLookupElementFactory
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

internal class FirBasicCompletionContext(
    val parameters: CompletionParameters,
    val result: CompletionResultSet,
    val prefixMatcher: PrefixMatcher,
    val originalKtFile: KtFile,
    val fakeKtFile: KtFile,
    val project: Project,
    val targetPlatform: TargetPlatform,
    val lookupElementFactory: KotlinFirLookupElementFactory = KotlinFirLookupElementFactory(),
) {
    companion object {
        fun createFromParameters(parameters: CompletionParameters, result: CompletionResultSet): FirBasicCompletionContext? {
            val prefixMatcher = result.prefixMatcher
            val originalKtFile = parameters.originalFile as? KtFile ?: return null
            val fakeKtFile = parameters.position.containingFile as? KtFile ?: return null
            val targetPlatform = TargetPlatformDetector.getPlatform(originalKtFile)
            val project = originalKtFile.project
            return FirBasicCompletionContext(parameters, result, prefixMatcher, originalKtFile, fakeKtFile, project, targetPlatform)
        }
    }
}