/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.findApplicableConfigurator
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.roots.invalidateProjectRoots
import org.jetbrains.kotlin.psi.KtFile

sealed class ChangeCoroutineSupportFix(
    element: PsiElement,
    coroutineSupport: LanguageFeature.State
) : AbstractChangeFeatureSupportLevelFix(element, LanguageFeature.Coroutines, coroutineSupport, shortFeatureName) {

    class InModule(element: PsiElement, coroutineSupport: LanguageFeature.State) : ChangeCoroutineSupportFix(element, coroutineSupport) {
        override fun getText() = "${super.getText()} in the current module"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return

            findApplicableConfigurator(module).changeCoroutineConfiguration(module, featureSupport)
        }
    }

    class InProject(element: PsiElement, coroutineSupport: LanguageFeature.State) : ChangeCoroutineSupportFix(element, coroutineSupport) {
        override fun getText() = "${super.getText()} in the project"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            if (featureSupportEnabled) {
                if (!checkUpdateRuntime(project, LanguageFeature.Coroutines.sinceApiVersion)) return
            }

            KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
                coroutinesState = when (featureSupport) {
                    LanguageFeature.State.ENABLED -> CommonCompilerArguments.ENABLE
                    LanguageFeature.State.ENABLED_WITH_WARNING -> CommonCompilerArguments.WARN
                    LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> CommonCompilerArguments.ERROR
                }
            }
            project.invalidateProjectRoots()
        }

    }

    companion object : FeatureSupportIntentionActionsFactory() {
        private const val shortFeatureName = "coroutine"

        fun getFixText(state: LanguageFeature.State) = AbstractChangeFeatureSupportLevelFix.getFixText(state, shortFeatureName)

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val module = ModuleUtilCore.findModuleForPsiElement(diagnostic.psiElement) ?: return emptyList()

            return doCreateActions(
                diagnostic, LanguageFeature.Coroutines, allowWarningAndErrorMode = true,
                quickFixConstructor = if (shouldConfigureInProject(module)) { element, _, coroutineSupport ->
                    InProject(
                        element,
                        coroutineSupport
                    )
                } else { element, _, coroutineSupport ->
                    InModule(element, coroutineSupport)
                }
            )
        }
    }
}
