/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const val VERSION_AND_TARGET_SEPARATOR = ':'
const val TARGETS_SEPARATOR = ','
const val VERSIONS_SEPARATOR = ','

internal fun TestServices.createUnmutingErrorIfNeeded(stringDirective: StringDirective, defaultLanguageVersion: LanguageVersion): List<Throwable> {
    return if (versionAndTargetAreIgnored(stringDirective, defaultLanguageVersion))
        listOf(
            AssertionError(
                "Looks like this test can be unmuted. Remove $defaultLanguageVersion from the $stringDirective directive"
            )
        )
    else emptyList()
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
