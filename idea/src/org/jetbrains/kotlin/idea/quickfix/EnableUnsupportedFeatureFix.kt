/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlFile
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.getRuntimeLibraryVersion
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.changeLanguageVersion
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.versions.findKotlinRuntimeLibrary
import org.jetbrains.kotlin.idea.versions.updateLibraries
import org.jetbrains.kotlin.psi.KtFile

sealed class EnableUnsupportedFeatureFix(
        element: PsiElement,
        protected val feature: LanguageFeature,
        protected val apiVersionOnly: Boolean
) : KotlinQuickFixAction<PsiElement>(element) {
    class InModule(element: PsiElement, feature: LanguageFeature, apiVersionOnly: Boolean) : EnableUnsupportedFeatureFix(element, feature, apiVersionOnly) {
        override fun getFamilyName() = "Increase module " + if (apiVersionOnly) "API version" else "language version"

        override fun getText() = if (apiVersionOnly)
            "Set module API version to ${feature.sinceApiVersion.versionString}"
        else
            "Set module language version to ${feature.sinceVersion!!.description}"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return
            val targetVersion = feature.sinceVersion!!

            val runtimeUpdateRequired = getRuntimeLibraryVersion(module)?.let { ApiVersion.parse(it) }?.let { runtimeVersion ->
               runtimeVersion < feature.sinceApiVersion
            } ?: false

            val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(module)
            val targetApiLevel = facetSettings.apiLevel?.let { apiLevel ->
                if (ApiVersion.createByLanguageVersion(apiLevel) < feature.sinceApiVersion)
                    feature.sinceApiVersion.versionString
                else
                    null
            }

            if (KotlinPluginUtil.isGradleModule(module) || KotlinPluginUtil.isMavenModule(module)) {
                if (runtimeUpdateRequired) {
                    Messages.showErrorDialog(project,
                                             "This language feature requires version ${feature.sinceApiVersion} or later of the Kotlin runtime library. " +
                                             "Please update the version in your build script.",
                                             "Update Language Version")
                    return
                }

                val element = if (KotlinPluginUtil.isGradleModule(module)) {
                    updateGradleLanguageVersion(module, file, targetApiLevel)
                }
                else {
                    updateMavenLanguageVersion(module, targetApiLevel)
                }
                element?.let {
                    OpenFileDescriptor(module.project, it.containingFile.virtualFile, it.textRange.startOffset).navigate(true)
                }
                return
            }

            if (runtimeUpdateRequired && !askUpdateRuntime(module, feature.sinceApiVersion)) {
                return
            }

            ModuleRootModificationUtil.updateModel(module) {
                with(facetSettings) {
                    if (!apiVersionOnly) {
                        languageLevel = targetVersion
                    }
                    if (targetApiLevel != null) {
                        apiLevel = LanguageVersion.fromVersionString(targetApiLevel)
                    }
                }
            }
        }

        private fun updateGradleLanguageVersion(module: Module, file: KtFile, targetApiLevel: String?): PsiElement? {
            val forTests = ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContent(file.virtualFile)
            return KotlinWithGradleConfigurator.changeLanguageVersion(module,
                if (apiVersionOnly) null else feature.sinceVersion!!.versionString,
                targetApiLevel, forTests)
        }

        private fun updateMavenLanguageVersion(module: Module, targetApiLevel: String?): PsiElement? {
            fun doUpdateMavenLanguageVersion(): PsiElement? {
                val psi = KotlinMavenConfigurator.findModulePomFile(module) as? XmlFile ?: return null
                val pom = PomFile.forFileOrNull(psi) ?: return null
                return pom.changeLanguageVersion(
                        if (apiVersionOnly) null else feature.sinceVersion!!.versionString,
                        targetApiLevel)
            }

            val element = doUpdateMavenLanguageVersion()
            if (element == null) {
                Messages.showErrorDialog(module.project,
                                         "Failed to update.pom.xml. Please update the file manually.",
                                         text)
            }
            return element
        }
    }

    class InProject(element: PsiElement, feature: LanguageFeature, apiVersionOnly: Boolean)
            : EnableUnsupportedFeatureFix(element, feature, apiVersionOnly)
    {
        override fun getFamilyName() = "Increase project " + if (apiVersionOnly) "API version" else "language version"

        override fun getText() = if (apiVersionOnly)
            "Set project API version to ${feature.sinceApiVersion.versionString}"
        else
            "Set project language version to ${feature.sinceVersion!!.versionString}"

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val targetVersion = feature.sinceVersion!!

            KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
                val parsedApiVersion = ApiVersion.parse(apiVersion!!)
                if (parsedApiVersion != null && feature.sinceApiVersion > parsedApiVersion) {
                    if (!checkUpdateRuntime(project, feature.sinceApiVersion)) return@update
                    apiVersion = feature.sinceApiVersion.versionString
                }

                if (!apiVersionOnly) {
                    languageVersion = targetVersion.versionString
                }
            }
            ProjectRootManagerEx.getInstanceEx(project).makeRootsChange({}, false, true)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): EnableUnsupportedFeatureFix? {
            val (feature, languageFeatureSettings) = Errors.UNSUPPORTED_FEATURE.cast(diagnostic).a

            val sinceVersion = feature.sinceVersion ?: return null
            val apiVersionOnly = sinceVersion <= languageFeatureSettings.languageVersion &&
                                 feature.sinceApiVersion > languageFeatureSettings.apiVersion

            if (!sinceVersion.isStable && !KotlinInternalMode.enabled) {
                return null
            }

            val module = ModuleUtilCore.findModuleForPsiElement(diagnostic.psiElement) ?: return null
            if (!KotlinPluginUtil.isGradleModule(module) && !KotlinPluginUtil.isMavenModule(module)) {
                val facetSettings = KotlinFacet.get(module)?.configuration?.settings
                if (facetSettings == null || facetSettings.useProjectSettings) return InProject(diagnostic.psiElement, feature, apiVersionOnly)
            }
            return InModule(diagnostic.psiElement, feature, apiVersionOnly)
        }
    }
}

fun checkUpdateRuntime(project: Project, requiredVersion: ApiVersion): Boolean {
    val modulesWithOutdatedRuntime = project.allModules().filter { module ->
        val parsedModuleRuntimeVersion = getRuntimeLibraryVersion(module)?.let { version ->
            ApiVersion.parse(version.substringBefore("-"))
        }
        parsedModuleRuntimeVersion != null && parsedModuleRuntimeVersion < requiredVersion
    }
    if (modulesWithOutdatedRuntime.isNotEmpty()) {
        if (!askUpdateRuntime(project, requiredVersion,
                              modulesWithOutdatedRuntime.mapNotNull { findKotlinRuntimeLibrary(it) })) return false
    }
    return true
}

fun askUpdateRuntime(project: Project, requiredVersion: ApiVersion, librariesToUpdate: List<Library>): Boolean {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
        val rc = Messages.showOkCancelDialog(project,
                                             "This language feature requires version $requiredVersion or later of the Kotlin runtime library. " +
                                             "Would you like to update the runtime library in your project?",
                                             "Update Runtime Library",
                                             Messages.getQuestionIcon())
        if (rc != Messages.OK) return false
    }

    updateLibraries(project, librariesToUpdate)
    return true
}

fun askUpdateRuntime(module: Module, requiredVersion: ApiVersion): Boolean {
    val library = findKotlinRuntimeLibrary(module) ?: return true
    return askUpdateRuntime(module.project, requiredVersion, listOf(library))
}
