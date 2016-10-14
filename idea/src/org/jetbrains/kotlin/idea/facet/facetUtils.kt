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

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import org.jetbrains.kotlin.idea.framework.getLibraryProperties
import org.jetbrains.kotlin.idea.maven.configuration.KotlinJavaMavenConfigurator
import org.jetbrains.kotlin.idea.maven.configuration.KotlinJavascriptMavenConfigurator
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion

private fun getRuntimeLibraryVersions(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: KotlinFacetConfiguration.TargetPlatform
): Collection<String> {
    val presentationProvider = when (targetPlatform) {
        KotlinFacetConfiguration.TargetPlatform.JS ->
            JSLibraryStdPresentationProvider.getInstance()
        KotlinFacetConfiguration.TargetPlatform.JVM_1_6,
        KotlinFacetConfiguration.TargetPlatform.JVM_1_8 ->
            JavaRuntimePresentationProvider.getInstance()
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

private fun getDefaultTargetPlatform(module: Module, rootModel: ModuleRootModel?): KotlinFacetConfiguration.TargetPlatform {
    if (getRuntimeLibraryVersions(module, rootModel, KotlinFacetConfiguration.TargetPlatform.JS).any()) {
        return KotlinFacetConfiguration.TargetPlatform.JS
    }

    val sdk = ((rootModel ?: ModuleRootManager.getInstance(module))).sdk
    val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
    return when {
        sdkVersion != null && sdkVersion <= JavaSdkVersion.JDK_1_6 -> KotlinFacetConfiguration.TargetPlatform.JVM_1_6
        else -> KotlinFacetConfiguration.TargetPlatform.JVM_1_8
    }
}

private fun getDefaultLanguageLevel(
        module: Module,
        explicitVersion: String? = null
): KotlinFacetConfiguration.LanguageLevel {
    val libVersion = explicitVersion
                     ?: KotlinVersionInfoProvider.EP_NAME.extensions
                             .mapNotNull { it.getCompilerVersion(module) }
                             .minWith(VersionComparatorUtil.COMPARATOR)
                     ?: bundledRuntimeVersion()
    return when {
        libVersion.startsWith("1.0") -> KotlinFacetConfiguration.LanguageLevel.KOTLIN_1_0
        else -> KotlinFacetConfiguration.LanguageLevel.KOTLIN_1_1
    }
}

internal fun getLibraryLanguageLevel(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: KotlinFacetConfiguration.TargetPlatform?
): KotlinFacetConfiguration.LanguageLevel {
    val minVersion = getRuntimeLibraryVersions(module, rootModel, targetPlatform ?: KotlinFacetConfiguration.TargetPlatform.JVM_1_8)
            .minWith(VersionComparatorUtil.COMPARATOR)
    return getDefaultLanguageLevel(module, minVersion)
}

internal fun KotlinFacetConfiguration.VersionInfo.initializeIfNeeded(module: Module, rootModel: ModuleRootModel?) {
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

internal fun Module.getKotlinVersionInfo(rootModel: ModuleRootModel? = null): KotlinFacetConfiguration.VersionInfo {
    val versionInfo = KotlinFacet.get(this)?.configuration?.state?.versionInfo ?: KotlinFacetConfiguration.VersionInfo()
    versionInfo.initializeIfNeeded(this, rootModel)
    return versionInfo
}

val KotlinFacetConfiguration.TargetPlatform.mavenLibraryId: String
    get() {
        return when (this) {
            KotlinFacetConfiguration.TargetPlatform.JVM_1_6,
            KotlinFacetConfiguration.TargetPlatform.JVM_1_8 ->
                KotlinJavaMavenConfigurator.STD_LIB_ID
            KotlinFacetConfiguration.TargetPlatform.JS ->
                KotlinJavascriptMavenConfigurator.STD_LIB_ID
        }
    }