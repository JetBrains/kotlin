/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class KotlinVersionConsistency {

    private val kotlinVersions = AllKotlinArgumentTypes().kotlinVersions
    private val supportedLanguageVersionsRange = LanguageVersion.FIRST_SUPPORTED..LanguageVersion.entries.last()
    private val supportedLanguageVersions = LanguageVersion.entries.filter {
        supportedLanguageVersionsRange.contains(it)
    }


    @Test
    fun allSupportedKotlinVersionsArePresent() {
        supportedLanguageVersions.forEach { languageVersion ->
            assertTrue(
                actual = languageVersion.toKotlinVersionOrNull() != null,
                message = "$languageVersion language version is not present in 'KotlinVersion' type"
            )
        }
    }

    @Test
    fun versionAreDeprecated() {
        supportedLanguageVersions
            .filter { it.isDeprecated }
            .forEach { languageVersion ->
                val kotlinVersion = languageVersion.toKotlinVersion()

                assertTrue(
                    actual = kotlinVersion.releaseVersionsMetadata.deprecatedVersion != null,
                    message = "LanguageVersion $languageVersion is deprecated, while $kotlinVersion KotlinVersion is not marked as deprecated"
                )
            }
    }

    @Test
    fun versionIsStabilized() {
        supportedLanguageVersions
            .filter { it.isStable }
            .forEach { languageVersion ->
                val kotlinVersion = languageVersion.toKotlinVersion()

                assertTrue(
                    actual = kotlinVersion.releaseVersionsMetadata.stabilizedVersion != null,
                    message = "LanguageVersion $languageVersion is stable, while $kotlinVersion KotlinVersion is not marked as stable"
                )
            }
    }

    @Test
    fun versionIsRemoved() {
        LanguageVersion.entries
            .filter { it.isUnsupported }
            .forEach { languageVersion ->
                languageVersion.toKotlinVersionOrNull()?.let {
                    assertTrue(
                        actual = it.releaseVersionsMetadata.removedVersion != null,
                        message = "LanguageVersion $languageVersion is not supported, while $it KotlinVersion is marked as supported"
                    )
                }
            }
    }

    @Test
    fun versionIsExperimental() {
        LanguageVersion.entries
            .filter { it > LanguageVersion.LATEST_STABLE }
            .forEach { languageVersion ->
                languageVersion.toKotlinVersionOrNull()?.let {
                    assertTrue(
                        actual = it.releaseVersionsMetadata.stabilizedVersion == null,
                        message = "LanguageVersion $languageVersion is not yet stable, while $it KotlinVersion is marked as stable"
                    )
                }
            }
    }

    private fun LanguageVersion.toKotlinVersion() = kotlinVersions.single { it.versionName == versionString }
    private fun LanguageVersion.toKotlinVersionOrNull() = kotlinVersions.singleOrNull { it.versionName == versionString }
}