/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.findApplicableConfigurator
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.roots.invalidateProjectRoots
import org.jetbrains.kotlin.psi.KtFile

sealed class ChangeCoroutineSupportFix(
        element: PsiElement,
        protected val coroutineSupport: LanguageFeature.State
) : KotlinQuickFixAction<PsiElement>(element) {
    protected val coroutineSupportEnabled: Boolean
            get() = coroutineSupport == LanguageFeature.State.ENABLED || coroutineSupport == LanguageFeature.State.ENABLED_WITH_WARNING

    class InModule(element: PsiElement, coroutineSupport: LanguageFeature.State) : ChangeCoroutineSupportFix(element, coroutineSupport) {
        override fun getText() = "${super.getText()} in the current module"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return

            findApplicableConfigurator(module).changeCoroutineConfiguration(module, coroutineSupport)
        }
    }

    class InProject(element: PsiElement, coroutineSupport: LanguageFeature.State) : ChangeCoroutineSupportFix(element, coroutineSupport) {
        override fun getText() = "${super.getText()} in the project"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            if (coroutineSupportEnabled) {
                if (!checkUpdateRuntime(project, LanguageFeature.Coroutines.sinceApiVersion)) return
            }

            KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
                coroutinesState = when (coroutineSupport) {
                    LanguageFeature.State.ENABLED -> CommonCompilerArguments.ENABLE
                    LanguageFeature.State.ENABLED_WITH_WARNING -> CommonCompilerArguments.WARN
                    LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> CommonCompilerArguments.ERROR
                }
            }
            project.invalidateProjectRoots()
        }

    }

    override fun getFamilyName() = "Enable/Disable coroutine support"

    override fun getText(): String {
        return getFixText(coroutineSupport)
    }

    companion object : KotlinIntentionActionsFactory() {
        fun getFixText(state: LanguageFeature.State): String {
            return when (state) {
                LanguageFeature.State.ENABLED -> "Enable coroutine support"
                LanguageFeature.State.ENABLED_WITH_WARNING -> "Enable coroutine support (with warning)"
                LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> "Disable coroutine support"
            }
        }

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val newCoroutineSupports = when (diagnostic.factory) {
                Errors.EXPERIMENTAL_FEATURE_ERROR -> {
                    if (Errors.EXPERIMENTAL_FEATURE_ERROR.cast(diagnostic).a.first != LanguageFeature.Coroutines) return emptyList()
                    listOf(LanguageFeature.State.ENABLED_WITH_WARNING, LanguageFeature.State.ENABLED)
                }
                Errors.EXPERIMENTAL_FEATURE_WARNING -> {
                    if (Errors.EXPERIMENTAL_FEATURE_WARNING.cast(diagnostic).a.first != LanguageFeature.Coroutines) return emptyList()
                    listOf(LanguageFeature.State.ENABLED, LanguageFeature.State.ENABLED_WITH_ERROR)
                }
                else -> return emptyList()
            }
            val module = ModuleUtilCore.findModuleForPsiElement(diagnostic.psiElement) ?: return emptyList()
            val facetSettings = KotlinFacet.get(module)?.configuration?.settings

            val configureInProject = (facetSettings == null || facetSettings.useProjectSettings) &&
                                     module.getBuildSystemType() == BuildSystemType.JPS
            val quickFixConstructor: (PsiElement, LanguageFeature.State) -> ChangeCoroutineSupportFix =
                    if (configureInProject) ::InProject else ::InModule
            return newCoroutineSupports.map { quickFixConstructor(diagnostic.psiElement, it) }
        }
    }
}
