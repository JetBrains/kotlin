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

package org.jetbrains.kotlin.idea.facet

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.cli.common.arguments.copyBean
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JsCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import org.jetbrains.kotlin.idea.versions.MAVEN_COMMON_STDLIB_ID
import org.jetbrains.kotlin.idea.versions.MAVEN_JS_STDLIB_ID
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion

private fun getRuntimeLibraryVersions(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: TargetPlatformKind<*>
): Collection<String> {
    val presentationProvider = when (targetPlatform) {
        is TargetPlatformKind.JavaScript -> JSLibraryStdPresentationProvider.getInstance()
        is TargetPlatformKind.Jvm -> JavaRuntimePresentationProvider.getInstance()
        is TargetPlatformKind.Default -> return emptyList()
    }

    KotlinVersionInfoProvider.EP_NAME
            .extensions
            .map { it.getLibraryVersions(module, targetPlatform) }
            .firstOrNull { it.isNotEmpty() }
            ?.let { return it }

    return (rootModel ?: ModuleRootManager.getInstance(module))
            .orderEntries
            .asSequence()
            .filterIsInstance<LibraryOrderEntry>()
            .mapNotNull { it.library?.let { presentationProvider.detect(it.getFiles(OrderRootType.CLASSES).toList())?.versionString } }
            .toList()
}

private fun getDefaultTargetPlatform(module: Module, rootModel: ModuleRootModel?): TargetPlatformKind<*> {
    if (getRuntimeLibraryVersions(module, rootModel, TargetPlatformKind.JavaScript).isNotEmpty()) {
        return TargetPlatformKind.JavaScript
    }

    val sdk = ((rootModel ?: ModuleRootManager.getInstance(module))).sdk
    val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
    return when {
        sdkVersion == null || sdkVersion >= JavaSdkVersion.JDK_1_8 -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_8]
        else -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
    }
}

private fun getDefaultLanguageLevel(
        module: Module,
        explicitVersion: String? = null
): LanguageVersion {
    val libVersion = explicitVersion
                     ?: KotlinVersionInfoProvider.EP_NAME.extensions
                             .mapNotNull { it.getCompilerVersion(module) }
                             .minWith(VersionComparatorUtil.COMPARATOR)
                     ?: bundledRuntimeVersion()
    return when {
        libVersion.startsWith("1.0") -> LanguageVersion.KOTLIN_1_0
        else -> LanguageVersion.KOTLIN_1_1
    }
}

internal fun getLibraryLanguageLevel(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: TargetPlatformKind<*>?
): LanguageVersion {
    val minVersion = getRuntimeLibraryVersions(module, rootModel, targetPlatform ?: TargetPlatformKind.Jvm[JvmTarget.JVM_1_8])
            .minWith(VersionComparatorUtil.COMPARATOR)
    return getDefaultLanguageLevel(module, minVersion)
}

fun KotlinFacetSettings.initializeIfNeeded(module: Module, rootModel: ModuleRootModel?) {
    val project = module.project

    with(versionInfo) {
        if (targetPlatformKind == null) {
            targetPlatformKind = getDefaultTargetPlatform(module, rootModel)
        }

        if (languageLevel == null) {
            languageLevel = getDefaultLanguageLevel(module)
        }

        if (apiLevel == null) {
            apiLevel = languageLevel!!.coerceAtMost(getLibraryLanguageLevel(module, rootModel, targetPlatformKind!!))
        }
    }

    with(compilerInfo) {
        if (commonCompilerArguments == null) {
            commonCompilerArguments = copyBean(KotlinCommonCompilerArgumentsHolder.getInstance(project).settings)
        }

        if (compilerSettings == null) {
            compilerSettings = copyBean(KotlinCompilerSettings.getInstance(project).settings)
        }

        if (k2jsCompilerArguments == null) {
            k2jsCompilerArguments = copyBean(Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings)
        }
    }
}

val TargetPlatformKind<*>.mavenLibraryId: String
    get() = when (this) {
        is TargetPlatformKind.Jvm -> MAVEN_STDLIB_ID
        is TargetPlatformKind.JavaScript -> MAVEN_JS_STDLIB_ID
        is TargetPlatformKind.Default -> MAVEN_COMMON_STDLIB_ID
    }

fun Module.getOrCreateFacet(modelsProvider: IdeModifiableModelsProvider): KotlinFacet {
    val facetModel = modelsProvider.getModifiableFacetModel(this)

    facetModel.findFacet(KotlinFacetType.TYPE_ID, KotlinFacetType.INSTANCE.defaultFacetName)?.let { return it }

    val facet = with(KotlinFacetType.INSTANCE) { createFacet(this@getOrCreateFacet, defaultFacetName, createDefaultConfiguration(), null) }
    facetModel.addFacet(facet)
    return facet
}

fun KotlinFacet.configureFacet(compilerVersion: String, modelsProvider: IdeModifiableModelsProvider) {
    val module = module
    with(configuration.settings) {
        versionInfo.targetPlatformKind = null
        versionInfo.apiLevel = null
        initializeIfNeeded(module, modelsProvider.getModifiableRootModel(module))
        with(versionInfo) {
            languageLevel = LanguageVersion.fromFullVersionString(compilerVersion) ?: LanguageVersion.LATEST
            // Both apiLevel and languageLevel should be initialized in the lines above
            if (apiLevel!! > languageLevel!!) {
                apiLevel = languageLevel
            }
        }
    }
}