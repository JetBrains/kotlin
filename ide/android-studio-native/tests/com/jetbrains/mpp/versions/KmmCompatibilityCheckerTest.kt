/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.versions

import com.jetbrains.kmm.versions.KmmCompatibilityChecker
import com.jetbrains.kmm.versions.KmmCompatibilityChecker.CompatibilityCheckResult
import com.jetbrains.kmm.versions.KmmCompatibilityChecker.CompatibilityCheckResult.*
import org.jetbrains.kotlin.idea.KotlinPluginVersion
import org.junit.Test
import org.junit.Assert.*

class KmmCompatibilityCheckerUnitTests {

    @Test
    fun `compatibilty range is exactly two versions of Kotlin forward`() {
        check(
            actualKotlinVersion = "1.4.0-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )

        check(
            actualKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.20-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.30-dev-1333-Studio3.5.1-2",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KMM_PLUGIN
        )
    }

    @Test
    fun `dev, eap or release of actual Kotlin doesnt matter`() {
        check(
            actualKotlinVersion = "1.4.20-dev-543-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.30-eap-322-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KMM_PLUGIN
        )

        check(
            actualKotlinVersion = "1.4.0-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-dev-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )

        check(
            actualKotlinVersion = "1.4.30-dev-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-dev-Studio3.5.1-1",
            expectedResult = OUTDATED_KMM_PLUGIN
        )
    }

    @Test
    fun `patch update of actual Kotlin doesnt matter`() {
        check(
            actualKotlinVersion = "1.4.02-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )

        check(
            actualKotlinVersion = "1.4.12-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.22-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.32-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KMM_PLUGIN
        )
    }

    @Test
    fun `patch version against which we compiled doesnt matter`() {
        check(
            actualKotlinVersion = "1.4.0-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.12-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )

        check(
            actualKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.12-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.13-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.12-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.20-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.12-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.30-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.12-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KMM_PLUGIN
        )
    }

    @Test
    fun `minor version should be the same`() {
        check(
            actualKotlinVersion = "1.4.70-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KMM_PLUGIN
        )

        check(
            actualKotlinVersion = "1.2.70-release-Studio3.5.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )
    }

    @Test
    fun `ide version doesnt matter`() {
        check(
            actualKotlinVersion = "1.4.0-release-Studio4.0.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )

        check(
            actualKotlinVersion = "1.4.10-release-Studio4.0.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.20-release-Studio4.0.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = COMPATIBLE
        )

        check(
            actualKotlinVersion = "1.4.30-release-Studio4.0.1-1",
            compiledAgainstKotlinVersion = "1.4.10-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KMM_PLUGIN
        )
    }

    @Test
    fun `1-3-7X versions are a special case and always incompatible`() {
        check(
            actualKotlinVersion = "1.3.70-release-Studio4.0.1-1",
            compiledAgainstKotlinVersion = "1.4.0-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )

        check(
            actualKotlinVersion = "1.3.71-release-Studio4.0.1-1",
            compiledAgainstKotlinVersion = "1.4.0-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )

        check(
            actualKotlinVersion = "1.3.72-release-Studio4.0.1-1",
            compiledAgainstKotlinVersion = "1.4.0-release-Studio3.5.1-1",
            expectedResult = OUTDATED_KOTLIN
        )
    }


    private fun check(actualKotlinVersion: String, compiledAgainstKotlinVersion: String, expectedResult: CompatibilityCheckResult) {
        val actualResult = KmmCompatibilityChecker.checkVersions(
            actualKotlinVersion,
            compiledAgainstKotlinVersion
        )
        assertEquals(expectedResult, actualResult)
    }
}