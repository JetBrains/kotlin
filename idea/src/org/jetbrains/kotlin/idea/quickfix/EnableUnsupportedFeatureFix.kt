/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.psi.KtFile

sealed class EnableUnsupportedFeatureFix(
        element: PsiElement,
        protected val targetVersion: LanguageVersion
) : KotlinQuickFixAction<PsiElement>(element) {
    class InModule(element: PsiElement, targetVersion: LanguageVersion) : EnableUnsupportedFeatureFix(element, targetVersion) {
        override fun getFamilyName() = "Increase module language level"

        override fun getText() = "Set module language level to ${targetVersion.versionString}"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return
            if (KotlinPluginUtil.isGradleModule(module)) {
                val forTests = ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContent(file.virtualFile)
                val element = KotlinWithGradleConfigurator.changeLanguageVersion(module, targetVersion.versionString, forTests)
                element?.let {
                    OpenFileDescriptor(project, it.containingFile.virtualFile, it.textRange.startOffset).navigate(true)
                }
                return
            }

            val facetSettings = KotlinFacet.get(module)?.configuration?.settings ?: return
            ModuleRootModificationUtil.updateModel(module) {
                with(facetSettings.versionInfo) {
                    languageLevel = targetVersion
                    apiLevel = targetVersion
                }
            }
        }
    }

    class InProject(element: PsiElement, targetVersion: LanguageVersion) : EnableUnsupportedFeatureFix(element, targetVersion) {
        override fun getFamilyName() = "Increase project language level"

        override fun getText() = "Set project language level to ${targetVersion.versionString}"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            with(KotlinCommonCompilerArgumentsHolder.getInstance(project).settings) {
                languageVersion = targetVersion.versionString
                apiVersion = targetVersion.versionString
            }
            ProjectRootManagerEx.getInstanceEx(project).makeRootsChange({}, false, true)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): EnableUnsupportedFeatureFix? {
            val targetVersion = Errors.UNSUPPORTED_FEATURE.cast(diagnostic).a.sinceVersion ?: return null
            val module = ModuleUtilCore.findModuleForPsiElement(diagnostic.psiElement) ?: return null
            if (KotlinPluginUtil.isMavenModule(module)) return null
            if (!KotlinPluginUtil.isGradleModule(module)) {
                val facetSettings = KotlinFacet.get(module)?.configuration?.settings
                if (facetSettings == null || facetSettings.useProjectSettings) return InProject(diagnostic.psiElement, targetVersion)
            }
            return InModule(diagnostic.psiElement, targetVersion)
        }
    }
}
