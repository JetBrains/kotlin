/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.VersionView
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.orDefault

interface KotlinVersionInfoProvider {
    companion object {
        val EP_NAME: ExtensionPointName<KotlinVersionInfoProvider> = ExtensionPointName.create("org.jetbrains.kotlin.versionInfoProvider")
    }

    fun getCompilerVersion(module: Module): String?
    fun getLibraryVersions(
        module: Module,
        platformKind: IdePlatformKind<*>,
        rootModel: ModuleRootModel?
    ): Collection<String>
}

fun getRuntimeLibraryVersions(
    module: Module,
    rootModel: ModuleRootModel?,
    platformKind: IdePlatformKind<*>
): Collection<String> = KotlinVersionInfoProvider.EP_NAME
    .extensions
    .map { it.getLibraryVersions(module, platformKind, rootModel) }
    .firstOrNull { it.isNotEmpty() } ?: emptyList()

fun getLibraryLanguageLevel(
    module: Module,
    rootModel: ModuleRootModel?,
    platformKind: IdePlatformKind<*>?,
    coerceRuntimeLibraryVersionToReleased: Boolean = true
): LanguageVersion {
    val minVersion = getRuntimeLibraryVersions(module, rootModel, platformKind.orDefault())
        .addReleaseVersionIfNecessary(coerceRuntimeLibraryVersionToReleased)
        .minWithOrNull(VersionComparatorUtil.COMPARATOR)
    return getDefaultLanguageLevel(module, minVersion, coerceRuntimeLibraryVersionToReleased)
}

fun getDefaultLanguageLevel(
    module: Module,
    explicitVersion: String? = null,
    coerceRuntimeLibraryVersionToReleased: Boolean = true
): LanguageVersion {
    val libVersion = explicitVersion
        ?: KotlinVersionInfoProvider.EP_NAME.extensions
            .mapNotNull { it.getCompilerVersion(module) }
            .addReleaseVersionIfNecessary(coerceRuntimeLibraryVersionToReleased)
            .minWithOrNull(VersionComparatorUtil.COMPARATOR)
        ?: return VersionView.RELEASED_VERSION
    return libVersion.toLanguageVersion()
}

fun String?.toLanguageVersion(): LanguageVersion = when {
    this == null -> VersionView.RELEASED_VERSION
    startsWith("1.4") -> LanguageVersion.KOTLIN_1_4
    startsWith("1.3") -> LanguageVersion.KOTLIN_1_3
    startsWith("1.2") -> LanguageVersion.KOTLIN_1_2
    startsWith("1.1") -> LanguageVersion.KOTLIN_1_1
    startsWith("1.0") -> LanguageVersion.KOTLIN_1_0
    else -> VersionView.RELEASED_VERSION
}

fun String?.toApiVersion(): ApiVersion = ApiVersion.createByLanguageVersion(toLanguageVersion())

private fun Iterable<String>.addReleaseVersionIfNecessary(shouldAdd: Boolean): Iterable<String> =
    if (shouldAdd) this + VersionView.RELEASED_VERSION.versionString else this

fun getRuntimeLibraryVersion(module: Module): String? {
    val settingsProvider = KotlinFacetSettingsProvider.getInstance(module.project) ?: return null
    val targetPlatform = settingsProvider.getInitializedSettings(module).targetPlatform
    val versions = getRuntimeLibraryVersions(module, null, targetPlatform.orDefault().idePlatformKind)
    return versions.toSet().singleOrNull()
}

fun getCleanRuntimeLibraryVersion(module: Module) = getRuntimeLibraryVersion(module)?.cleanUpVersion()

private fun String.cleanUpVersion(): String {
    return StringBuilder(this)
        .apply {
            val parIndex = indexOf("(")
            if (parIndex >= 0) {
                delete(parIndex, length)
            }
            val releaseIndex = indexOf("-release-")
            if (releaseIndex >= 0) {
                delete(releaseIndex, length)
            }
        }
        .toString()
        .trim()
}
