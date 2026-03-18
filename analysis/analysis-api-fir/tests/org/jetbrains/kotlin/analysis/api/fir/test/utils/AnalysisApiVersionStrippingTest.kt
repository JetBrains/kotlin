/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.utils

import org.jetbrains.kotlin.analysis.test.framework.utils.stripOutKotlinVersionFromFileName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnalysisApiVersionStrippingTest {
    private companion object {
        val COMMON_VERSION = KotlinVersion(2, 3, 0)
    }

    @Test
    fun releaseVersion() = test("kotlin-stdlib-2.3.0.jar", "kotlin-stdlib.jar")

    @Test
    fun releaseVersionWithoutExtension() = test("kotlin-stdlib-2.3.0", "kotlin-stdlib")

    @Test
    fun snapshotVersion() = test("kotlin-stdlib-2.3.0-SNAPSHOT.jar", "kotlin-stdlib.jar")

    @Test
    fun devVersion() = test("kotlin-stdlib-2.3.0-dev-1234.jar", "kotlin-stdlib.jar")

    @Test
    fun rcVersion() = test("kotlin-stdlib-2.3.0-RC3.jar", "kotlin-stdlib.jar")

    @Test
    fun snapshotVersionWithoutExtension() = test("kotlin-stdlib-2.3.0-SNAPSHOT", "kotlin-stdlib")

    @Test
    fun noVersionInFileName() = test("kotlin-stdlib.jar", "kotlin-stdlib.jar")

    @Test
    fun versionMismatch() = test("kotlin-stdlib-2.3.1.jar", "kotlin-stdlib-2.3.1.jar")

    @Test
    fun jarEntryPath() = test(
        "kotlin-stdlib-2.3.0.jar!/kotlin/collections/Collections.class",
        "kotlin-stdlib.jar!/kotlin/collections/Collections.class",
    )

    @Test
    fun rcVersionWithJarEntryPath() = test(
        "kotlin-stdlib-2.3.0-RC2.jar!/some/File.class",
        "kotlin-stdlib.jar!/some/File.class",
    )

    private fun test(fileName: String, expectedResult: String, version: KotlinVersion = COMMON_VERSION) {
        val actualResult = stripOutKotlinVersionFromFileName(fileName, version)
        assertEquals(expectedResult, actualResult)
    }
}