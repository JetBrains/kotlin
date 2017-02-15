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
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JsCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.versions.*
import java.lang.reflect.Field

private fun getDefaultTargetPlatform(module: Module, rootModel: ModuleRootModel?): TargetPlatformKind<*> {
    if (getRuntimeLibraryVersions(module, rootModel, TargetPlatformKind.JavaScript).isNotEmpty()) {
        return TargetPlatformKind.JavaScript
    }
    if (getRuntimeLibraryVersions(module, rootModel, TargetPlatformKind.Common).isNotEmpty()) {
        return TargetPlatformKind.Common
    }

    val sdk = ((rootModel ?: ModuleRootManager.getInstance(module))).sdk
    val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
    return when {
        sdkVersion == null || sdkVersion >= JavaSdkVersion.JDK_1_8 -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_8]
        else -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
    }
}

fun KotlinFacetSettings.initializeIfNeeded(module: Module, rootModel: ModuleRootModel?) {
    val project = module.project

    val commonArguments = KotlinCommonCompilerArgumentsHolder.getInstance(module.project).settings

    with(versionInfo) {
        if (targetPlatformKind == null) {
            targetPlatformKind = getDefaultTargetPlatform(module, rootModel)
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

    with(compilerInfo) {
        if (commonCompilerArguments == null) {
            commonCompilerArguments = copyBean(commonArguments)
        }

        if (compilerSettings == null) {
            compilerSettings = copyBean(KotlinCompilerSettings.getInstance(project).settings)
        }

        if (k2jsCompilerArguments == null) {
            k2jsCompilerArguments = copyBean(Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings)
        }

        if (k2jvmCompilerArguments == null) {
            k2jvmCompilerArguments = copyBean(Kotlin2JvmCompilerArgumentsHolder.getInstance(project).settings)
        }
    }
}

val TargetPlatformKind<*>.mavenLibraryIds: List<String>
    get() = when (this) {
        is TargetPlatformKind.Jvm -> listOf(MAVEN_STDLIB_ID, MAVEN_STDLIB_ID_JRE7, MAVEN_STDLIB_ID_JRE8)
        is TargetPlatformKind.JavaScript -> listOf(MAVEN_JS_STDLIB_ID, MAVEN_OLD_JS_STDLIB_ID)
        is TargetPlatformKind.Common -> listOf(MAVEN_COMMON_STDLIB_ID)
    }

fun Module.getOrCreateFacet(modelsProvider: IdeModifiableModelsProvider, useProjectSettings: Boolean): KotlinFacet {
    val facetModel = modelsProvider.getModifiableFacetModel(this)

    val facet = facetModel.findFacet(KotlinFacetType.TYPE_ID, KotlinFacetType.INSTANCE.defaultFacetName)
                ?: with(KotlinFacetType.INSTANCE) { createFacet(this@getOrCreateFacet, defaultFacetName, createDefaultConfiguration(), null) }
                        .apply { facetModel.addFacet(this) }
    facet.configuration.settings.useProjectSettings = useProjectSettings
    return facet
}

fun KotlinFacet.configureFacet(
        compilerVersion: String,
        coroutineSupport: CoroutineSupport,
        platformKind: TargetPlatformKind<*>?, // if null, detect by module dependencies
        modelsProvider: IdeModifiableModelsProvider
) {
    val module = module
    with(configuration.settings) {
        versionInfo.targetPlatformKind = platformKind
        versionInfo.apiLevel = null
        initializeIfNeeded(module, modelsProvider.getModifiableRootModel(module))
        with(versionInfo) {
            languageLevel = LanguageVersion.fromFullVersionString(compilerVersion) ?: LanguageVersion.LATEST
            // Both apiLevel and languageLevel should be initialized in the lines above
            if (apiLevel!! > languageLevel!!) {
                apiLevel = languageLevel
            }
        }
        compilerInfo.coroutineSupport = coroutineSupport
        compilerInfo.commonCompilerArguments?.languageVersion = versionInfo.languageLevel!!.versionString
        compilerInfo.commonCompilerArguments?.apiVersion = versionInfo.apiLevel!!.versionString
    }
}

// Update these lists when facet/project settings UI changes
private val commonExposedFields = listOf("languageVersion",
                                         "apiVersion",
                                         "suppressWarnings",
                                         "coroutinesEnable",
                                         "coroutinesWarn",
                                         "coroutinesError")
private val jvmExposedFields = commonExposedFields +
                               listOf("jvmTarget")
private val jsExposedFields = commonExposedFields +
                              listOf("sourceMap",
                                     "outputPrefix",
                                     "outputPostfix",
                                     "moduleKind")

private val CommonCompilerArguments.exposedFields: List<String>
    get() = when (this) {
        is K2JVMCompilerArguments -> jvmExposedFields
        is K2JSCompilerArguments -> jsExposedFields
        else -> commonExposedFields
    }

fun parseCompilerArgumentsToFacet(arguments: List<String>, defaultArguments: List<String>, kotlinFacet: KotlinFacet) {
    val argumentArray = arguments.toTypedArray()

    with(kotlinFacet.configuration.settings) {
        // todo: merge common arguments with platform-specific ones in facet settings

        val commonCompilerArguments = compilerInfo.commonCompilerArguments!!
        val compilerArguments = when (versionInfo.targetPlatformKind) {
            is TargetPlatformKind.Jvm -> compilerInfo.k2jvmCompilerArguments
            is TargetPlatformKind.JavaScript -> compilerInfo.k2jsCompilerArguments
            else -> commonCompilerArguments
        }!!

        val defaultCompilerArguments = compilerArguments.javaClass.newInstance()
        parseArguments(defaultArguments.toTypedArray(), defaultCompilerArguments, true)

        val oldCoroutineSupport = CoroutineSupport.byCompilerArguments(commonCompilerArguments)
        commonCompilerArguments.coroutinesEnable = false
        commonCompilerArguments.coroutinesWarn = false
        commonCompilerArguments.coroutinesError = false

        if (compilerArguments != commonCompilerArguments) {
            compilerArguments.coroutinesEnable = false
            compilerArguments.coroutinesWarn = false
            compilerArguments.coroutinesError = false
        }

        parseArguments(argumentArray, compilerArguments, true)

        val restoreCoroutineSupport =
                !compilerArguments.coroutinesEnable && !compilerArguments.coroutinesWarn && !compilerArguments.coroutinesError

        compilerArguments.apiVersion?.let { versionInfo.apiLevel = LanguageVersion.fromVersionString(it) }
        compilerArguments.languageVersion?.let { versionInfo.languageLevel = LanguageVersion.fromVersionString(it) }

        if (versionInfo.targetPlatformKind is TargetPlatformKind.Jvm) {
            val jvmTarget = compilerInfo.k2jvmCompilerArguments!!.jvmTarget
            if (jvmTarget != null) {
                versionInfo.targetPlatformKind = TargetPlatformKind.Jvm.JVM_PLATFORMS.firstOrNull {
                    VersionComparatorUtil.compare(it.version.description, jvmTarget) >= 0
                } ?: TargetPlatformKind.Jvm.JVM_PLATFORMS.last()
            }
        }

        // Retain only fields exposed in facet configuration editor.
        // The rest is combined into string and stored in CompilerSettings.additionalArguments

        val exposedFields = compilerArguments.exposedFields

        fun exposeAsAdditionalArgument(field: Field) = field.name !in exposedFields && field.get(compilerArguments) != field.get(defaultCompilerArguments)

        val additionalArgumentsString = with(compilerArguments.javaClass.newInstance()) {
            copyFieldsSatisfying(compilerArguments, this, ::exposeAsAdditionalArgument)
            ArgumentUtils.convertArgumentsToStringList(this).joinToString(separator = " ")
        }
        compilerInfo.compilerSettings!!.additionalArguments =
                if (additionalArgumentsString.isNotEmpty()) additionalArgumentsString else CompilerSettings.DEFAULT_ADDITIONAL_ARGUMENTS

        with(compilerArguments.javaClass.newInstance()) {
            copyFieldsSatisfying(this, compilerArguments, ::exposeAsAdditionalArgument)
        }

        copyInheritedFields(compilerArguments, commonCompilerArguments)

        if (restoreCoroutineSupport) {
            compilerInfo.coroutineSupport = oldCoroutineSupport
        }
    }
}