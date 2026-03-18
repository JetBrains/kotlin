/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.jetbrains.kotlin.config.JvmTarget as CompilerJvmTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmTargetConsistency {
    @Test
    fun supportedVersionsRangeDescriptionIsConsistent() {
        val lastSupportedTarget = CompilerJvmTarget.entries.last()
        assertTrue("Please update the max supported target in the `JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION`") {
            JvmTarget.CURRENT_SUPPORTED_VERSIONS_DESCRIPTION.endsWith(lastSupportedTarget.description)
        }

        val firstSupportedTarget = CompilerJvmTarget.supportedValues().first()
        assertTrue("Please update the min supported target in the `JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION`") {
            JvmTarget.CURRENT_SUPPORTED_VERSIONS_DESCRIPTION.startsWith(firstSupportedTarget.description)
        }
    }

    @Test
    fun defaultJvmTargetIsConsistent() {
        val defaultTarget = CompilerJvmTarget.DEFAULT
        assertEquals(
            defaultTarget.description,
            JvmTarget.CURRENT_DEFAULT_VERSION
        )
    }

    @Test
    fun allSupportedKotlinVersionsArePresent() {
        CompilerJvmTarget.entries.forEach { jvmTarget ->
            assertTrue(
                actual = jvmTarget.toDslJvmTargetOrNull() != null,
                message = "Jvm target '$jvmTarget' is not present in 'JvmTarget' DSL type"
            )
        }
    }

    @Test
    fun versionIsRemoved() {
        (CompilerJvmTarget.entries - CompilerJvmTarget.supportedValues())
            .forEach { jvmTarget ->
                jvmTarget.toDslJvmTargetOrNull()?.let {
                    assertTrue(
                        actual = it.releaseVersionsMetadata.removedVersion != null,
                        message = "Jvm target '$jvmTarget' is not supported, while $it 'removedVersion' is 'null'!"
                    )
                }
            }
    }

    private fun CompilerJvmTarget.toDslJvmTargetOrNull() = JvmTarget.entries.find { target ->
        target.targetName == description
    }
}
