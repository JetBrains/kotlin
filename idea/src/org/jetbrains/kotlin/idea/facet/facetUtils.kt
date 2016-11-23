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
import org.jetbrains.kotlin.cli.common.arguments.copyBean
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JsCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import org.jetbrains.kotlin.idea.framework.getLibraryProperties
import org.jetbrains.kotlin.idea.maven.configuration.KotlinJavaMavenConfigurator
import org.jetbrains.kotlin.idea.maven.configuration.KotlinJavascriptMavenConfigurator

private fun getRuntimeLibraryVersions(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: TargetPlatformKind<*>
): Collection<String> {
    val presentationProvider = when (targetPlatform) {
        is JSPlatform -> JSLibraryStdPresentationProvider.getInstance()
        is JVMPlatform -> JavaRuntimePresentationProvider.getInstance()
        else -> error("Unexpected platform: $targetPlatform")
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
            .mapNotNull { it.library?.let { getLibraryProperties(presentationProvider, it) }?.versionString }
            .toList()
}

private fun getDefaultTargetPlatform(module: Module, rootModel: ModuleRootModel?): TargetPlatformKind<*> {
    if (getRuntimeLibraryVersions(module, rootModel, JSPlatform).any()) {
        return JSPlatform
    }

    val sdk = ((rootModel ?: ModuleRootManager.getInstance(module))).sdk
    val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
    return when {
        sdkVersion == null || sdkVersion >= JavaSdkVersion.JDK_1_8 -> JVMPlatform[JvmTarget.JVM_1_6]
        else -> JVMPlatform[JvmTarget.JVM_1_6]
    }
}

fun KotlinFacetSettings.initializeIfNeeded(module: Module, rootModel: ModuleRootModel?) {
    val project = module.project

    with(versionInfo) {
        if (targetPlatformKindKind == null) {
            targetPlatformKindKind = getDefaultTargetPlatform(module, rootModel)
        }

        if (languageLevel == null) {
            languageLevel = LanguageVersion.KOTLIN_1_0
        }

        if (apiLevel == null) {
            apiLevel = LanguageVersion.KOTLIN_1_0
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
    get() {
        return when (this) {
            is JVMPlatform -> KotlinJavaMavenConfigurator.STD_LIB_ID
            is JSPlatform -> KotlinJavascriptMavenConfigurator.STD_LIB_ID
            else -> error("Unexpected platform: $this")
        }
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
        versionInfo.targetPlatformKindKind = null
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
