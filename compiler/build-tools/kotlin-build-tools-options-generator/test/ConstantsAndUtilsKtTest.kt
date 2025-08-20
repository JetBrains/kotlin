/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.options.generator.getOldestSupportedVersion
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Test class for the `getOldestSupportedVersion` function from `ConstantsAndUtilsKt`.
 *
 * The `getOldestSupportedVersion` function determines the oldest supported major Kotlin release version
 * from the given Kotlin release version based on a configured window of backwards compatibility.
 */
class ConstantsAndUtilsKtTest {

    @Test
    fun `should return the same version when there are no older major versions`() {
        val inputVersion = KotlinReleaseVersion.v1_0_4
        val result = getOldestSupportedVersion(inputVersion)
        assertEquals(KotlinReleaseVersion.v1_0_0, result)
    }

    @Test
    fun `should return the oldest major version while staying within the support limit`() {
        val inputVersion = KotlinReleaseVersion.v2_3_0
        val result = getOldestSupportedVersion(inputVersion)
        assertEquals(KotlinReleaseVersion.v2_0_0, result)
    }

    @Test
    fun `should correctly calculate the oldest supported version for mid-range input`() {
        val inputVersion = KotlinReleaseVersion.v1_9_24
        val result = getOldestSupportedVersion(inputVersion)
        assertEquals(KotlinReleaseVersion.v1_6_0, result)
    }

    @Test
    fun `should handle case when crossing 1_x to 2_x versions`() {
        val inputVersion = KotlinReleaseVersion.v2_2_20
        val result = getOldestSupportedVersion(inputVersion)
        assertEquals(KotlinReleaseVersion.v1_9_0, result)
    }

    @Test
    fun `should return correct oldest version for input with non-zero patch number`() {
        val inputVersion = KotlinReleaseVersion.v1_8_22
        val result = getOldestSupportedVersion(inputVersion)
        assertEquals(KotlinReleaseVersion.v1_5_0, result)
    }
}