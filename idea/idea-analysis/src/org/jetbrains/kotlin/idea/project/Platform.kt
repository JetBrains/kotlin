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
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.facet.getLibraryLanguageLevel
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.DefaultIdeTargetPlatformKindProvider
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.isCommon
import org.jetbrains.kotlin.utils.Jsr305State
import java.io.File

val KtElement.platform: TargetPlatform
    get() = TargetPlatformDetector.getPlatform(containingKtFile)

val KtElement.builtIns: KotlinBuiltIns
    get() = getResolutionFacade().moduleDescriptor.builtIns

var KtFile.forcedTargetPlatform: TargetPlatform? by UserDataProperty(Key.create("FORCED_TARGET_PLATFORM"))

fun Module.getAndCacheLanguageLevelByDependencies(): LanguageVersion {
    val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this)
    val languageLevel = getLibraryLanguageLevel(this, null, facetSettings.platform?.idePlatformKind)

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
    } else {
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

/**
 * Returns stable binary name of module from the *Kotlin* point of view.
 * Having correct module name is critical for compiler, e.g. for 'internal'-visibility
 * mangling (see KT-23668).
 *
 * Note that build systems and IDEA have their own module systems and, potentially, their
 * names can be different from Kotlin module name (though this is the rare case).
 */
fun Module.getStableName(): Name {
    // Here we check ideal situation: we have a facet, and it has 'moduleName' argument.
    // This should be the case for the most environments
    val arguments = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this).mergedCompilerArguments
    val explicitNameFromArguments = when (arguments) {
        is K2JVMCompilerArguments -> arguments.moduleName
        is K2JSCompilerArguments -> arguments.outputFile?.let { FileUtil.getNameWithoutExtension(File(it)) }
        is K2MetadataCompilerArguments -> arguments.moduleName
        else -> null // Actually, only 'null' possible here
    }

    // Here we handle pessimistic case: no facet is found or it declares no 'moduleName'
    // We heuristically assume that name of Module in IDEA is the same as Kotlin module (which may be not the case)
    val stableNameApproximation = explicitNameFromArguments ?: name

    return Name.special("<$stableNameApproximation>")
}

@JvmOverloads
fun Project.getLanguageVersionSettings(
    contextModule: Module? = null,
    jsr305State: Jsr305State? = null,
    isReleaseCoroutines: Boolean? = null
): LanguageVersionSettings {
    val arguments = KotlinCommonCompilerArgumentsHolder.getInstance(this).settings
    val languageVersion =
        LanguageVersion.fromVersionString(arguments.languageVersion)
            ?: contextModule?.getAndCacheLanguageLevelByDependencies()
            ?: VersionView.RELEASED_VERSION
    val apiVersion = ApiVersion.createByLanguageVersion(LanguageVersion.fromVersionString(arguments.apiVersion) ?: languageVersion)
    val compilerSettings = KotlinCompilerSettings.getInstance(this).settings

    val additionalArguments: CommonCompilerArguments = parseArguments(
        DefaultIdeTargetPlatformKindProvider.defaultPlatform,
        compilerSettings.additionalArgumentsAsList
    )

    val extraLanguageFeatures = additionalArguments.configureLanguageFeatures(MessageCollector.NONE).apply {
        configureCoroutinesSupport(
            CoroutineSupport.byCompilerArguments(KotlinCommonCompilerArgumentsHolder.getInstance(this@getLanguageVersionSettings).settings),
            languageVersion
        )
        if (isReleaseCoroutines != null) {
            put(
                LanguageFeature.ReleaseCoroutines,
                if (isReleaseCoroutines) LanguageFeature.State.ENABLED else LanguageFeature.State.DISABLED
            )
        }
    }

    val extraAnalysisFlags = additionalArguments.configureAnalysisFlags(MessageCollector.NONE).apply {
        if (jsr305State != null) put(JvmAnalysisFlags.jsr305, jsr305State)
    }

    return LanguageVersionSettingsImpl(
        languageVersion, apiVersion,
        arguments.configureAnalysisFlags(MessageCollector.NONE) + extraAnalysisFlags,
        arguments.configureLanguageFeatures(MessageCollector.NONE) + extraLanguageFeatures
    )
}

private val LANGUAGE_VERSION_SETTINGS = Key.create<CachedValue<LanguageVersionSettings>>("LANGUAGE_VERSION_SETTINGS")

val Module.languageVersionSettings: LanguageVersionSettings
    get() {
        val cachedValue =
            getUserData(LANGUAGE_VERSION_SETTINGS)
                ?: createCachedValueForLanguageVersionSettings().also { putUserData(LANGUAGE_VERSION_SETTINGS, it) }

        return cachedValue.value
    }

private fun Module.createCachedValueForLanguageVersionSettings(): CachedValue<LanguageVersionSettings> {
    return CachedValuesManager.getManager(project).createCachedValue({
                                                                         CachedValueProvider.Result(
                                                                             computeLanguageVersionSettings(),
                                                                             ProjectRootModificationTracker.getInstance(
                                                                                 project
                                                                             )
                                                                         )
                                                                     }, false)
}

private fun Module.shouldUseProjectLanguageVersionSettings(): Boolean {
    val facetSettingsProvider = KotlinFacetSettingsProvider.getInstance(project)
    return facetSettingsProvider.getSettings(this) == null || facetSettingsProvider.getInitializedSettings(this).useProjectSettings
}

private fun Module.computeLanguageVersionSettings(): LanguageVersionSettings {
    if (shouldUseProjectLanguageVersionSettings()) return project.getLanguageVersionSettings()

    val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this)
    val languageVersion = facetSettings.languageLevel ?: getAndCacheLanguageLevelByDependencies()
    val apiVersion = facetSettings.apiLevel ?: languageVersion

    val languageFeatures = facetSettings.mergedCompilerArguments?.configureLanguageFeatures(MessageCollector.NONE)?.apply {
        configureCoroutinesSupport(facetSettings.coroutineSupport, languageVersion)
        configureMultiplatformSupport(facetSettings.platform?.idePlatformKind, this@computeLanguageVersionSettings)
    }.orEmpty()

    val analysisFlags = facetSettings.mergedCompilerArguments?.configureAnalysisFlags(MessageCollector.NONE).orEmpty()

    return LanguageVersionSettingsImpl(
        languageVersion,
        ApiVersion.createByLanguageVersion(apiVersion),
        analysisFlags,
        languageFeatures
    )
}

val Module.platform: TargetPlatform?
    get() = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this).platform ?: project.platform

// TODO: check it, it does some madness
val Project.platform: TargetPlatform?
    get() {
        val jvmTarget = Kotlin2JvmCompilerArgumentsHolder.getInstance(this).settings.jvmTarget ?: return null
        val version = JvmTarget.fromString(jvmTarget) ?: return null
        return DefaultBuiltInPlatforms.jvmPlatformByTargetVersion(version)
    }

private val Module.implementsCommonModule: Boolean
    get() = !platform.isCommon() // FIXME(dsavvinov): this doesn't seems right, in multilevel-MPP 'common' modules can implement other commons
            && ModuleRootManager.getInstance(this).dependencies.any { it.platform.isCommon() }

private fun parseArguments(
    platformKind: TargetPlatform,
    additionalArguments: List<String>
): CommonCompilerArguments {
    return platformKind.createArguments { parseCommandLineArguments(additionalArguments, this) }
}

fun MutableMap<LanguageFeature, LanguageFeature.State>.configureCoroutinesSupport(
    coroutineSupport: LanguageFeature.State?,
    languageVersion: LanguageVersion
) {
    val state = if (languageVersion >= LanguageVersion.KOTLIN_1_3) {
        LanguageFeature.State.ENABLED
    } else {
        coroutineSupport ?: LanguageFeature.Coroutines.defaultState
    }
    put(LanguageFeature.Coroutines, state)
}

fun MutableMap<LanguageFeature, LanguageFeature.State>.configureMultiplatformSupport(
    platformKind: IdePlatformKind<*>?,
    module: Module?
) {
    if (platformKind.isCommon || module?.implementsCommonModule == true) {
        put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED)
    }
}


val PsiElement.languageVersionSettings: LanguageVersionSettings
    get() {
        if (ServiceManager.getService(project, ProjectFileIndex::class.java) == null) {
            return LanguageVersionSettingsImpl.DEFAULT
        }
        return IDELanguageSettingsProvider.getLanguageVersionSettings(this.getModuleInfo(), project)
    }