/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.context

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.completion.KotlinFirCompletionParameters
import org.jetbrains.kotlin.idea.completion.LookupElementSink
import org.jetbrains.kotlin.idea.completion.lookups.factories.KotlinFirLookupElementFactory
import org.jetbrains.kotlin.idea.fir.HLIndexHelper
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

internal class FirBasicCompletionContext(
    val parameters: CompletionParameters,
    val sink: LookupElementSink,
    val prefixMatcher: PrefixMatcher,
    val originalKtFile: KtFile,
    val fakeKtFile: KtFile,
    val project: Project,
    val targetPlatform: TargetPlatform,
    val indexHelper: HLIndexHelper,
    val lookupElementFactory: KotlinFirLookupElementFactory = KotlinFirLookupElementFactory(),
) {
    val visibleScope = KotlinSourceFilterScope.projectSourceAndClassFiles(originalKtFile.resolveScope, project)
    val moduleInfo: IdeaModuleInfo = originalKtFile.getModuleInfo()

    companion object {
        fun createFromParameters(firParameters: KotlinFirCompletionParameters, result: CompletionResultSet): FirBasicCompletionContext? {
            val prefixMatcher = result.prefixMatcher
            val parameters = firParameters.ijParameters
            val originalKtFile = parameters.originalFile as? KtFile ?: return null
            val fakeKtFile = parameters.position.containingFile as? KtFile ?: return null
            val targetPlatform = TargetPlatformDetector.getPlatform(originalKtFile)
            val project = originalKtFile.project
            val indexHelper = createIndexHelper(parameters)
            return FirBasicCompletionContext(
                parameters,
                LookupElementSink(result, firParameters),
                prefixMatcher,
                originalKtFile,
                fakeKtFile,
                project,
                targetPlatform,
                indexHelper
            )
        }

        private fun createIndexHelper(parameters: CompletionParameters) = HLIndexHelper(
            parameters.position.project,
            parameters.position.getModuleInfo().contentScope()
        )
    }
}