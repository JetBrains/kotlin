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

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.facet.getLibraryLanguageLevel
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.TargetPlatform

val KtElement.platform: TargetPlatform
    get() = TargetPlatformDetector.getPlatform(containingKtFile)

val KtElement.builtIns: KotlinBuiltIns
    get() = getResolutionFacade().moduleDescriptor.builtIns

private val multiPlatformProjectsArg: String by lazy {
    CommonCompilerArguments::multiPlatform.annotations.filterIsInstance<Argument>().single().value
}

fun Module.getAndCacheLanguageLevelByDependencies(): LanguageVersion {
    val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this)
    val languageLevel = getLibraryLanguageLevel(this, null, facetSettings.targetPlatformKind)

    // Preserve inferred version in facet/project settings
    if (facetSettings.useProjectSettings) {
        KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
            if (languageVersion == null) {
                languageVersion = languageLevel.versionString
            }
            if (apiVersion == null) {
                apiVersion = languageLevel.versionString
            }
        }
    }
    else {
        with(facetSettings) {
            if (this.languageLevel == null) {
                this.languageLevel = languageLevel
            }
            if (this.apiLevel == null) {
                this.apiLevel = languageLevel
            }
        }
    }

    return languageLevel
}

@JvmOverloads
fun Project.getLanguageVersionSettings(contextModule: Module? = null,
                                       extraAnalysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap()): LanguageVersionSettings {
    val arguments = KotlinCommonCompilerArgumentsHolder.getInstance(this).settings
    val languageVersion =
            LanguageVersion.fromVersionString(arguments.languageVersion)
            ?: contextModule?.getAndCacheLanguageLevelByDependencies()
            ?: LanguageVersion.LATEST_STABLE
    val apiVersion = ApiVersion.createByLanguageVersion(LanguageVersion.fromVersionString(arguments.apiVersion) ?: languageVersion)
    val compilerSettings = KotlinCompilerSettings.getInstance(this).settings
    val extraLanguageFeatures = getExtraLanguageFeatures(
            TargetPlatformKind.Common,
            CoroutineSupport.byCompilerArguments(KotlinCommonCompilerArgumentsHolder.getInstance(this).settings),
            compilerSettings,
            null
    )
    return LanguageVersionSettingsImpl(languageVersion, apiVersion,
                                       arguments.configureAnalysisFlags() + extraAnalysisFlags,
                                       extraLanguageFeatures)
}

val Module.languageVersionSettings: LanguageVersionSettings
    get() {
        val facetSettingsProvider = KotlinFacetSettingsProvider.getInstance(project)
        if (facetSettingsProvider.getSettings(this) == null) return project.getLanguageVersionSettings(this)
        val facetSettings = facetSettingsProvider.getInitializedSettings(this)
        if (facetSettings.useProjectSettings) return project.getLanguageVersionSettings(this)
        val languageVersion = facetSettings.languageLevel ?: getAndCacheLanguageLevelByDependencies()
        val apiVersion = facetSettings.apiLevel ?: languageVersion

        val extraLanguageFeatures = getExtraLanguageFeatures(
                facetSettings.targetPlatformKind ?: TargetPlatformKind.Common,
                facetSettings.coroutineSupport,
                facetSettings.compilerSettings,
                this
        )

        val arguments = facetSettings.compilerArguments
        if (arguments != null) {
            facetSettings.compilerSettings?.let { compilerSettings ->
                parseCommandLineArguments(compilerSettings.additionalArgumentsAsList, arguments)
            }
        }

        return LanguageVersionSettingsImpl(
                languageVersion,
                ApiVersion.createByLanguageVersion(apiVersion),
                arguments?.configureAnalysisFlags().orEmpty(),
                extraLanguageFeatures
        )
    }

val Module.targetPlatform: TargetPlatformKind<*>?
    get() = KotlinFacetSettingsProvider.getInstance(project).getSettings(this)?.targetPlatformKind ?: project.targetPlatform

val Project.targetPlatform: TargetPlatformKind<*>?
    get() {
        val jvmTarget = Kotlin2JvmCompilerArgumentsHolder.getInstance(this).settings.jvmTarget ?: return null
        val version = JvmTarget.fromString(jvmTarget) ?: return null
        return TargetPlatformKind.Jvm[version]
    }

private val Module.implementsCommonModule: Boolean
    get() = targetPlatform != TargetPlatformKind.Common
            && ModuleRootManager.getInstance(this).dependencies.any { it.targetPlatform == TargetPlatformKind.Common }

private fun getExtraLanguageFeatures(
        targetPlatformKind: TargetPlatformKind<*>,
        coroutineSupport: LanguageFeature.State,
        compilerSettings: CompilerSettings?,
        module: Module?
): Map<LanguageFeature, LanguageFeature.State> {
    return mutableMapOf<LanguageFeature, LanguageFeature.State>().apply {
        put(LanguageFeature.Coroutines, coroutineSupport)
        if (targetPlatformKind == TargetPlatformKind.Common ||
            // TODO: this is a dirty hack, parse arguments correctly here
            compilerSettings?.additionalArguments?.contains(multiPlatformProjectsArg) == true ||
            (module != null && module.implementsCommonModule)) {
            put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED)
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

val KtElement.jvmTarget: JvmTarget
    get() {
        if (ServiceManager.getService(containingKtFile.project, ProjectFileIndex::class.java) == null) {
            return JvmTarget.DEFAULT
        }
        return ModuleUtilCore.findModuleForPsiElement(this)?.targetPlatform?.version as? JvmTarget ?: JvmTarget.DEFAULT
    }
