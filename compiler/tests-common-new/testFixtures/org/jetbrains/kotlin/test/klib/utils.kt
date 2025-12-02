/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.targetPlatform
import kotlin.text.startsWith

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const val VERSION_AND_TARGET_SEPARATOR = '_'

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
 * Check whether defaultLanguageVersion and `target platform name` match to value of ignore directive in format `<LV>..._<TARGETPLATFORM>`,
 * or `<LV>...` (matches to any target platform)
 */
internal fun TestServices.versionAndTargetAreIgnored(directive: StringDirective, defaultLanguageVersion: LanguageVersion): Boolean {
    val firstModule = moduleStructure.modules.first()
    val versionString = defaultLanguageVersion.versionString
    val filteredByVersion = firstModule.directives[directive].filter { it.startsWith(versionString) }

    if (filteredByVersion.isEmpty()) return false
    filteredByVersion.filter { it.startsWith(versionString) }.forEach {
        if (!it.contains(VERSION_AND_TARGET_SEPARATOR))
            return true // A version without a target is treated as ignore for all targets.
    }
    val platformName = firstModule.targetPlatform(this).componentPlatforms.single().platformName
    return filteredByVersion.any { it.startsWith(versionString) && it.endsWith("$VERSION_AND_TARGET_SEPARATOR$platformName") }
}
