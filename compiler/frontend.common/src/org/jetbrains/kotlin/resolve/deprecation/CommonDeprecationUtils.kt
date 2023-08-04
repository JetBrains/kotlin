/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement

fun VersionRequirement.isFulfilled(languageVersionSettings: LanguageVersionSettings): Boolean {
    val requiredVersion = createVersion(version.asString())

    val currentVersion = when (kind) {
        ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION ->
            MavenComparableVersion(languageVersionSettings.languageVersion.versionString)
        ProtoBuf.VersionRequirement.VersionKind.API_VERSION ->
            languageVersionSettings.apiVersion.version
        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION ->
            KotlinCompilerVersion.getVersion()?.substringBefore('-')?.let(::createVersion)
        else -> null
    }

    return currentVersion == null || currentVersion >= requiredVersion
}

private fun createVersion(version: String): MavenComparableVersion? = try {
    MavenComparableVersion(version)
} catch (e: Exception) {
    null
}
