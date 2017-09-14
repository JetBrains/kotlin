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

package org.jetbrains.kotlin.idea.facet

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JsCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.*
import kotlin.reflect.KProperty1

private fun getDefaultTargetPlatform(module: Module, rootModel: ModuleRootModel?): TargetPlatformKind<*> {
    for (platform in TargetPlatformKind.ALL_PLATFORMS) {
        if (platform.version == TargetPlatformVersion.NoVersion &&
            getRuntimeLibraryVersions(module, rootModel, platform).isNotEmpty()) {
            return platform
        }
    }

    val sdk = ((rootModel ?: ModuleRootManager.getInstance(module))).sdk
    val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
    return when {
        sdkVersion == null || sdkVersion >= JavaSdkVersion.JDK_1_8 -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_8]
        else -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
    }
}

fun KotlinFacetSettings.initializeIfNeeded(
        module: Module,
        rootModel: ModuleRootModel?,
        platformKind: TargetPlatformKind<*>? = null // if null, detect by module dependencies
) {
    val project = module.project

    if (compilerSettings == null) {
        compilerSettings = KotlinCompilerSettings.getInstance(project).settings
    }

    val commonArguments = KotlinCommonCompilerArgumentsHolder.getInstance(module.project).settings

    if (compilerArguments == null) {
        val targetPlatformKind = platformKind ?: getDefaultTargetPlatform(module, rootModel)
        compilerArguments = targetPlatformKind.createCompilerArguments {
            targetPlatformKind.getPlatformCompilerArgumentsByProject(module.project)?.let { mergeBeans(it, this) }
            mergeBeans(commonArguments, this)
        }
    }

    if (languageLevel == null) {
        languageLevel = (if (useProjectSettings) LanguageVersion.fromVersionString(commonArguments.languageVersion) else null)
                        ?: getDefaultLanguageLevel(module)
    }

    if (apiLevel == null) {
        apiLevel = if (useProjectSettings) {
            LanguageVersion.fromVersionString(commonArguments.apiVersion) ?: languageLevel
        }
        else {
            languageLevel!!.coerceAtMost(getLibraryLanguageLevel(module, rootModel, targetPlatformKind!!))
        }
    }
}

fun TargetPlatformKind<*>.getPlatformCompilerArgumentsByProject(project: Project): CommonCompilerArguments? {
    return when (this) {
        is TargetPlatformKind.Jvm -> Kotlin2JvmCompilerArgumentsHolder.getInstance(project).settings
        is TargetPlatformKind.JavaScript -> Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings
        else -> null
    }
}

val TargetPlatformKind<*>.mavenLibraryIds: List<String>
    get() = when (this) {
        is TargetPlatformKind.Jvm -> listOf(MAVEN_STDLIB_ID, MAVEN_STDLIB_ID_JRE7, MAVEN_STDLIB_ID_JRE8)
        is TargetPlatformKind.JavaScript -> listOf(MAVEN_JS_STDLIB_ID, MAVEN_OLD_JS_STDLIB_ID)
        is TargetPlatformKind.Common -> listOf(MAVEN_COMMON_STDLIB_ID)
    }

val mavenLibraryIdToPlatform: Map<String, TargetPlatformKind<*>> by lazy {
    TargetPlatformKind.ALL_PLATFORMS
            .flatMap { platform -> platform.mavenLibraryIds.map { it to platform } }
            .sortedByDescending { it.first.length }
            .toMap()
}

fun Module.getOrCreateFacet(modelsProvider: IdeModifiableModelsProvider,
                            useProjectSettings: Boolean,
                            commitModel: Boolean = false): KotlinFacet {
    val facetModel = modelsProvider.getModifiableFacetModel(this)

    val facet = facetModel.findFacet(KotlinFacetType.TYPE_ID, KotlinFacetType.INSTANCE.defaultFacetName)
                ?: with(KotlinFacetType.INSTANCE) { createFacet(this@getOrCreateFacet, defaultFacetName, createDefaultConfiguration(), null) }
                        .apply { facetModel.addFacet(this) }
    facet.configuration.settings.useProjectSettings = useProjectSettings
    if (commitModel) {
        runWriteAction {
            facetModel.commit()
        }
    }
    return facet
}

fun KotlinFacet.configureFacet(
        compilerVersion: String,
        coroutineSupport: LanguageFeature.State,
        platformKind: TargetPlatformKind<*>?, // if null, detect by module dependencies
        modelsProvider: IdeModifiableModelsProvider
) {
    val module = module
    with(configuration.settings) {
        compilerArguments = null
        compilerSettings = null
        initializeIfNeeded(module, modelsProvider.getModifiableRootModel(module), platformKind)
        languageLevel = LanguageVersion.fromFullVersionString(compilerVersion) ?: LanguageVersion.LATEST_STABLE
        // Both apiLevel and languageLevel should be initialized in the lines above
        if (apiLevel!! > languageLevel!!) {
            apiLevel = languageLevel
        }
        this.coroutineSupport = coroutineSupport
    }
}

// "Primary" fields are written to argument beans directly and thus not presented in the "additional arguments" string
// Update these lists when facet/project settings UI changes
val commonUIExposedFields = listOf(CommonCompilerArguments::languageVersion.name,
                                   CommonCompilerArguments::apiVersion.name,
                                   CommonCompilerArguments::suppressWarnings.name,
                                   CommonCompilerArguments::coroutinesState.name)
private val commonUIHiddenFields = listOf(CommonCompilerArguments::pluginClasspaths.name,
                                          CommonCompilerArguments::pluginOptions.name)
private val commonPrimaryFields = commonUIExposedFields + commonUIHiddenFields

private val jvmSpecificUIExposedFields = listOf(K2JVMCompilerArguments::jvmTarget.name,
                                                K2JVMCompilerArguments::destination.name,
                                                K2JVMCompilerArguments::classpath.name)
val jvmUIExposedFields = commonUIExposedFields + jvmSpecificUIExposedFields
private val jvmPrimaryFields = commonPrimaryFields + jvmSpecificUIExposedFields

private val jsSpecificUIExposedFields = listOf(K2JSCompilerArguments::sourceMap.name,
                                               K2JSCompilerArguments::sourceMapPrefix.name,
                                               K2JSCompilerArguments::sourceMapEmbedSources.name,
                                               K2JSCompilerArguments::outputPrefix.name,
                                               K2JSCompilerArguments::outputPostfix.name,
                                               K2JSCompilerArguments::moduleKind.name)
val jsUIExposedFields = commonUIExposedFields + jsSpecificUIExposedFields
private val jsPrimaryFields = commonPrimaryFields + jsSpecificUIExposedFields

private val CommonCompilerArguments.primaryFields: List<String>
    get() = when (this) {
        is K2JVMCompilerArguments -> jvmPrimaryFields
        is K2JSCompilerArguments -> jsPrimaryFields
        else -> commonPrimaryFields
    }

private val CommonCompilerArguments.ignoredFields: List<String>
    get() = when (this) {
        is K2JVMCompilerArguments -> listOf(K2JVMCompilerArguments::noJdk.name, K2JVMCompilerArguments::jdkHome.name)
        else -> emptyList()
    }

private fun Module.configureJdkIfPossible(compilerArguments: K2JVMCompilerArguments, modelsProvider: IdeModifiableModelsProvider) {
    val jdkHome = compilerArguments.jdkHome ?: return
    val jdk = ProjectJdkTable.getInstance().allJdks.firstOrNull {
        it.sdkType is JavaSdk && FileUtil.comparePaths(it.homePath, jdkHome) == 0
    } ?: return
    modelsProvider.getModifiableRootModel(this).sdk = jdk
}

fun parseCompilerArgumentsToFacet(
        arguments: List<String>,
        defaultArguments: List<String>,
        kotlinFacet: KotlinFacet,
        modelsProvider: IdeModifiableModelsProvider
) {
    with(kotlinFacet.configuration.settings) {
        val compilerArguments = this.compilerArguments ?: return

        val defaultCompilerArguments = compilerArguments::class.java.newInstance()
        parseCommandLineArguments(defaultArguments, defaultCompilerArguments)
        defaultCompilerArguments.convertPathsToSystemIndependent()

        parseCommandLineArguments(arguments, compilerArguments)

        compilerArguments.convertPathsToSystemIndependent()

        // Retain only fields exposed (and not explicitly ignored) in facet configuration editor.
        // The rest is combined into string and stored in CompilerSettings.additionalArguments

        if (compilerArguments is K2JVMCompilerArguments) {
            kotlinFacet.module.configureJdkIfPossible(compilerArguments, modelsProvider)
        }

        val primaryFields = compilerArguments.primaryFields
        val ignoredFields = compilerArguments.ignoredFields

        fun exposeAsAdditionalArgument(property: KProperty1<CommonCompilerArguments, Any?>) =
                property.name !in primaryFields && property.get(compilerArguments) != property.get(defaultCompilerArguments)

        val additionalArgumentsString = with(compilerArguments::class.java.newInstance()) {
            copyFieldsSatisfying(compilerArguments, this) { exposeAsAdditionalArgument(it) && it.name !in ignoredFields }
            ArgumentUtils.convertArgumentsToStringList(this).joinToString(separator = " ") {
                if (StringUtil.containsWhitespaces(it) || it.startsWith('"')) {
                    StringUtil.wrapWithDoubleQuote(StringUtil.escapeQuotes(it))
                } else it
            }
        }
        compilerSettings?.additionalArguments =
                if (additionalArgumentsString.isNotEmpty()) additionalArgumentsString else CompilerSettings.DEFAULT_ADDITIONAL_ARGUMENTS

        with(compilerArguments::class.java.newInstance()) {
            copyFieldsSatisfying(this, compilerArguments) { exposeAsAdditionalArgument(it) || it.name in ignoredFields }
        }

        updateMergedArguments()
    }
}
