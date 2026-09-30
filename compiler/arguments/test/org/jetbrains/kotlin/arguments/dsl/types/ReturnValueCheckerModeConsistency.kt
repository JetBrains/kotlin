/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.jetbrains.kotlin.arguments.description.actualCommonCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.kotlin.config.ReturnValueCheckerMode as CompilerReturnValueCheckerMode

class ReturnValueCheckerModeConsistency {

    @Test
    fun optionsHaveSeparateLifecycles() {
        val oldOption = actualCommonCompilerArguments.arguments.single { it.name == "Xreturn-value-checker" }
        val stableOption = actualCommonCompilerArguments.arguments.single { it.name == "return-value-checker" }

        assertEquals(KotlinReleaseVersion.v2_2_0, oldOption.releaseVersionsMetadata.introducedVersion)
        assertEquals(KotlinReleaseVersion.v2_5_0, oldOption.releaseVersionsMetadata.deprecatedVersion)
        assertNull(oldOption.releaseVersionsMetadata.removedVersion)
        assertEquals(KotlinReleaseVersion.v2_5_0, stableOption.releaseVersionsMetadata.introducedVersion)
        assertEquals(KotlinReleaseVersion.v2_5_0, stableOption.releaseVersionsMetadata.stabilizedVersion)
        assertNull(stableOption.deprecatedName)
    }

    @Test
    fun allCompilerTypeValuesArePresent() {
        CompilerReturnValueCheckerMode.entries.forEach { entry ->
            assertTrue(
                actual = ReturnValueCheckerMode.entries.any { it.modeState == entry.state },
                message = "Missing entry $entry in DSL types"
            )
        }
    }
}
