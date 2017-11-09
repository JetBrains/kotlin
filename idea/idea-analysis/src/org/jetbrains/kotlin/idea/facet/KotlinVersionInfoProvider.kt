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

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.TargetPlatformKind

interface KotlinVersionInfoProvider {
    companion object {
        val EP_NAME: ExtensionPointName<KotlinVersionInfoProvider> = ExtensionPointName.create("org.jetbrains.kotlin.versionInfoProvider")
    }

    fun getCompilerVersion(module: Module): String?
    fun getLibraryVersions(
            module: Module,
            targetPlatform: TargetPlatformKind<*>,
            rootModel: ModuleRootModel?
    ): Collection<String>
}

fun getRuntimeLibraryVersions(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: TargetPlatformKind<*>
): Collection<String> {
    return KotlinVersionInfoProvider.EP_NAME
                   .extensions
                   .map { it.getLibraryVersions(module, targetPlatform, rootModel) }
                   .firstOrNull { it.isNotEmpty() } ?: emptyList()
}

fun getLibraryLanguageLevel(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: TargetPlatformKind<*>?
): LanguageVersion {
    val minVersion = getRuntimeLibraryVersions(module, rootModel, targetPlatform ?: TargetPlatformKind.DEFAULT_PLATFORM)
            .minWith(VersionComparatorUtil.COMPARATOR)
    return getDefaultLanguageLevel(module, minVersion)
}

fun getDefaultLanguageLevel(
        module: Module,
        explicitVersion: String? = null
): LanguageVersion {
    val libVersion = explicitVersion
                     ?: KotlinVersionInfoProvider.EP_NAME.extensions
                             .mapNotNull { it.getCompilerVersion(module) }
                             .minWith(VersionComparatorUtil.COMPARATOR)
                     ?: return LanguageVersion.LATEST_STABLE
    return when {
        libVersion.startsWith("1.3") -> LanguageVersion.KOTLIN_1_3
        libVersion.startsWith("1.2") -> LanguageVersion.KOTLIN_1_2
        libVersion.startsWith("1.1") -> LanguageVersion.KOTLIN_1_1
        libVersion.startsWith("1.0") -> LanguageVersion.KOTLIN_1_0
        else -> LanguageVersion.LATEST_STABLE
    }
}

fun getRuntimeLibraryVersion(module: Module): String? {
    val targetPlatform = KotlinFacetSettingsProvider.getInstance(module.project).getInitializedSettings(module).targetPlatformKind
    val versions = getRuntimeLibraryVersions(module, null, targetPlatform ?: TargetPlatformKind.DEFAULT_PLATFORM)
    return versions.toSet().singleOrNull()
}
