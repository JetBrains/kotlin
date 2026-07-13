/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.RegisteredDirectivesBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_MULTIPLE_API_VERSIONS_SETTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.API_VERSION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.junit.jupiter.api.Assumptions

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const val VERSION_AND_TARGET_SEPARATOR = ':'
const val TARGETS_SEPARATOR = ','
const val VERSIONS_SEPARATOR = ','

internal fun TestServices.throwUnmutingErrorIfNeeded(stringDirective: StringDirective, defaultLanguageVersion: LanguageVersion) {
    if (versionAndTargetAreIgnored(stringDirective, defaultLanguageVersion)) {
        throw AssertionError("Looks like this test can be unmuted. Remove $defaultLanguageVersion from the $stringDirective directive")
    }
}

/**
 * Check whether defaultLanguageVersion and `target platform name` match to one of values of ignore directive in formats:
 * - `<VERSION>`, for ex., `1.9.20` or `2.0` or `2.2.21` or `*`
 * - `<TARGETPLATFORM_LIST>:<VERSION>`, for ex., `JS:2.0` or `JS,Native:*` or `JS,Wasm:2.2.20` or `ANY:1.9` or `ANY:2.0,2.1`
 */
internal fun TestServices.versionAndTargetAreIgnored(directive: StringDirective, defaultLanguageVersion: LanguageVersion): Boolean {
    val firstModule = moduleStructure.modules.first()
    val versionString = defaultLanguageVersion.versionString

    for (maybeTuple in firstModule.directives[directive]) {
        val parts = maybeTuple.split(VERSION_AND_TARGET_SEPARATOR)
        // Check for a matching version or ANY version
        val lastPart = parts.last()
        if (lastPart == "*" || lastPart.split(VERSIONS_SEPARATOR).any {
                it == versionString || it.substringAfter(versionString).matches(Regex("\\.\\d+"))
            }) {
            when (parts.size) {
                1 -> return true // no platform specification means any platform
                2 -> { // Check for a matching platform or ANY platform
                    if (parts[0] == "ANY") return true
                    val targets = parts[0].split(TARGETS_SEPARATOR).map { it.uppercase() }

                    @OptIn(TestInfrastructureInternals::class)
                    val componentPlatformNames = defaultsProvider.targetPlatform.componentPlatforms.map {
                        it.platformName.uppercase()
                    }
                    if (componentPlatformNames.any(targets::contains))
                        return true
                }
                else -> error("Cannot parse `$maybeTuple`. See KDoc for `TestServices.versionAndTargetAreIgnored()`")
            }
        }
    }
    return false
}

/**
 * Set up the necessary directives for a KLIB compatibility test to enforce running it under
 * a specific [customLanguageVersion] and the relevant API version.
 */
fun RegisteredDirectivesBuilder.setupCustomLanguageVersionForKlibCompatibilityTest(customLanguageVersion: LanguageVersion) {
    +ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
    LANGUAGE_VERSION with customLanguageVersion
    +ALLOW_MULTIPLE_API_VERSIONS_SETTING
    API_VERSION with ApiVersion.createByLanguageVersion(customLanguageVersion)
}

/**
 * Set up the necessary directives for a forward KLIB compatibility test to enforce running it under
 * a specific [secondStageCompilerDefaultLanguageVersion] and the relevant API version.
 */
fun RegisteredDirectivesBuilder.setupCustomLVForKlibForwardCompatibilityTest(secondStageCompilerDefaultLanguageVersion: LanguageVersion) {
    val firstStageCompilerDefaultLanguageVersion = LanguageVersion.LATEST_STABLE

    when {
        secondStageCompilerDefaultLanguageVersion == firstStageCompilerDefaultLanguageVersion -> {
            // OK, versions match.
        }

        secondStageCompilerDefaultLanguageVersion == LanguageVersion.entries[firstStageCompilerDefaultLanguageVersion.ordinal - 1] -> {
            // We need to set the custom LV to let `UnsupportedFeaturesTestConfigurator` skip tests with
            // the language features that are not supported in the given custom LV.
            setupCustomLanguageVersionForKlibCompatibilityTest(secondStageCompilerDefaultLanguageVersion)

            LANGUAGE with "+ExportKlibToOlderAbiVersion"
        }

        secondStageCompilerDefaultLanguageVersion < firstStageCompilerDefaultLanguageVersion -> {
            // Too old LV, we don't support exporting in it.
            // In case there are still such tests, don't make them fails - just ignore them all.
            Assumptions.abort<Nothing>()
        }

        else -> error(
            """
                Incompatible combination of default language versions:
                - 1st stage compiler default LV: $firstStageCompilerDefaultLanguageVersion
                - 2nd stage compiler default LV: $secondStageCompilerDefaultLanguageVersion
            """.trimIndent()
        )
    }
}
