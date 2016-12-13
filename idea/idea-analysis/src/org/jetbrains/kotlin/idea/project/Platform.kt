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

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.sampullara.cli.Argument
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.TargetPlatform

val KtElement.platform: TargetPlatform
    get() = TargetPlatformDetector.getPlatform(getContainingKtFile())

val KtElement.builtIns: KotlinBuiltIns
    get() = getResolutionFacade().moduleDescriptor.builtIns

private val multiPlatformProjectsArg: String by lazy {
    "-" + CommonCompilerArguments::multiPlatform.annotations.filterIsInstance<Argument>().single().value
}

val Project.languageVersionSettings: LanguageVersionSettings
    get() {
        val arguments = KotlinCommonCompilerArgumentsHolder.getInstance(this).settings
        val languageVersion = LanguageVersion.fromVersionString(arguments.languageVersion) ?: LanguageVersion.LATEST
        val apiVersion = ApiVersion.createByLanguageVersion(LanguageVersion.fromVersionString(arguments.apiVersion) ?: languageVersion)
        val compilerSettings = KotlinCompilerSettings.getInstance(this).settings
        val extraLanguageFeatures = getExtraLanguageFeatures(
                TargetPlatformKind.Default,
                CoroutineSupport.byCompilerArguments(KotlinCommonCompilerArgumentsHolder.getInstance(this).settings),
                compilerSettings
        )
        return LanguageVersionSettingsImpl(
                languageVersion,
                apiVersion,
                extraLanguageFeatures
        )
    }

val Module.languageVersionSettings: LanguageVersionSettings
    get() {
        val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getSettings(this)
        if (facetSettings.useProjectSettings) return project.languageVersionSettings
        val versionInfo = facetSettings.versionInfo
        val languageVersion = versionInfo.languageLevel ?: LanguageVersion.LATEST
        val apiVersion = versionInfo.apiLevel ?: languageVersion

        val extraLanguageFeatures = getExtraLanguageFeatures(
                versionInfo.targetPlatformKind ?: TargetPlatformKind.Default,
                facetSettings.compilerInfo.coroutineSupport,
                facetSettings.compilerInfo.compilerSettings
        )

        return LanguageVersionSettingsImpl(languageVersion, ApiVersion.createByLanguageVersion(apiVersion), extraLanguageFeatures)
    }

private fun getExtraLanguageFeatures(
        targetPlatformKind: TargetPlatformKind<*>,
        coroutineSupport: CoroutineSupport,
        compilerSettings: CompilerSettings?
): List<LanguageFeature> {
    return mutableListOf<LanguageFeature>().apply {
        when (coroutineSupport) {
            CoroutineSupport.ENABLED -> {}
            CoroutineSupport.ENABLED_WITH_WARNING -> add(LanguageFeature.WarnOnCoroutines)
            CoroutineSupport.DISABLED -> add(LanguageFeature.ErrorOnCoroutines)
        }
        if (targetPlatformKind == TargetPlatformKind.Default ||
            // TODO: this is a dirty hack, parse arguments correctly here
            compilerSettings?.additionalArguments?.contains(multiPlatformProjectsArg) == true) {
            add(LanguageFeature.MultiPlatformProjects)
        }
    }
}

val KtElement.languageVersionSettings: LanguageVersionSettings
    get() {
        if (ServiceManager.getService(containingKtFile.project, ProjectFileIndex::class.java) == null) {
            return LanguageVersionSettingsImpl.DEFAULT
        }
        return ModuleUtilCore.findModuleForPsiElement(this)?.languageVersionSettings ?: LanguageVersionSettingsImpl.DEFAULT
    }
