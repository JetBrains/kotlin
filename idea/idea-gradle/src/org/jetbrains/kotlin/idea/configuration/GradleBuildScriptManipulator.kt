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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.plugins.gradle.settings.GradleSettings

interface GradleBuildScriptManipulator<out Psi : PsiFile> {
    val scriptFile: Psi
    val preferNewSyntax: Boolean

    fun isConfiguredWithOldSyntax(kotlinPluginName: String): Boolean
    fun isConfigured(kotlinPluginExpression: String): Boolean

    fun configureModuleBuildScript(
        kotlinPluginName: String,
        kotlinPluginExpression: String,
        stdlibArtifactName: String,
        version: String,
        jvmTarget: String?
    ): Boolean

    fun configureProjectBuildScript(kotlinPluginName: String, version: String): Boolean

    fun changeCoroutineConfiguration(coroutineOption: String): PsiElement?

    fun changeLanguageFeatureConfiguration(feature: LanguageFeature, state: LanguageFeature.State, forTests: Boolean): PsiElement?

    fun changeLanguageVersion(version: String, forTests: Boolean): PsiElement?

    fun changeApiVersion(version: String, forTests: Boolean): PsiElement?

    fun addKotlinLibraryToModuleBuildScript(
        scope: DependencyScope,
        libraryDescriptor: ExternalLibraryDescriptor
    )

    fun getKotlinStdlibVersion(): String?

    // For settings.gradle/settings.gradle.kts

    fun addMavenCentralPluginRepository()
    fun addPluginRepository(repository: RepositoryDescription)

    fun addResolutionStrategy(pluginId: String)
}

fun fetchGradleVersion(psiFile: PsiFile): GradleVersion {
    return gradleVersionFromFile(psiFile) ?: GradleVersion.current()
}

private fun gradleVersionFromFile(psiFile: PsiFile): GradleVersion? {
    val module = psiFile.module ?: return null
    val path = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
    return GradleSettings.getInstance(module.project).getLinkedProjectSettings(path)?.resolveGradleVersion()
}

val MIN_GRADLE_VERSION_FOR_NEW_PLUGIN_SYNTAX = GradleVersion.version("4.4")

fun GradleBuildScriptManipulator<*>.useNewSyntax(kotlinPluginName: String, gradleVersion: GradleVersion): Boolean {
    if (!preferNewSyntax) return false

    if (gradleVersion < MIN_GRADLE_VERSION_FOR_NEW_PLUGIN_SYNTAX) return false

    if (isConfiguredWithOldSyntax(kotlinPluginName)) return false

    val fileText = runReadAction { scriptFile.text }
    val hasOldApply = fileText.contains("apply plugin:")

    return !hasOldApply
}

private val MIN_GRADLE_VERSION_FOR_API_AND_IMPLEMENTATION = GradleVersion.version("3.4")

fun GradleVersion.scope(directive: String): String {
    if (this < MIN_GRADLE_VERSION_FOR_API_AND_IMPLEMENTATION) {
        return when (directive) {
            "implementation" -> "compile"
            "testImplementation" -> "testCompile"
            else -> throw IllegalArgumentException("Unknown directive `$directive`")
        }
    }

    return directive
}