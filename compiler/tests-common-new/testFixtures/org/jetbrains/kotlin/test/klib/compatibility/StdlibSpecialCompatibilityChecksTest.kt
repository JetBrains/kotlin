/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib.compatibility

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.test.klib.compatibility.LibrarySpecialCompatibilityChecksTest.Companion.SORTED_TEST_COMPILER_VERSION_GROUPS
import org.jetbrains.kotlin.test.klib.compatibility.LibrarySpecialCompatibilityChecksTest.Companion.SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

interface StdlibSpecialCompatibilityChecksTest : DummyLibraryCompiler {
    @Test
    fun testExportToOlderAbiVersionWithOlderLibrary() {
        for (compilerVersion in SORTED_TEST_COMPILER_VERSION_GROUPS.flatten()) {
            for (libraryVersion in SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS) {
                compileDummyLibrary(
                    libraryVersion = libraryVersion,
                    compilerVersion = compilerVersion,
                    expectedWarningStatus = WarningStatus.NO_WARNINGS,
                    exportKlibToOlderAbiVersion = true,
                )
            }
        }
    }

    @Test
    fun testExportToOlderAbiVersionWithCurrentLibrary() {
        for (compilerVersion in SORTED_TEST_COMPILER_VERSION_GROUPS.flatten()) {
            for (libraryVersion in SORTED_TEST_COMPILER_VERSION_GROUPS.flatten()) {
                compileDummyLibrary(
                    libraryVersion = libraryVersion,
                    compilerVersion = compilerVersion,
                    expectedWarningStatus = WarningStatus.TOO_NEW_LIBRARY_WARNING,
                    exportKlibToOlderAbiVersion = true,
                )
            }
        }
    }

    @Test
    fun `check compiler DEV version is properly detected`() {
        listOf(
            "2.5.0" to false,
            "2.5.0-1" to false,
            "2.5.0-release-123" to false,
            "2.5.0-M1" to false,
            "2.5.0-Alpha2" to false,
            "2.5.0-Beta1" to false,
            "2.5.0-RC" to false,
            "2.5.0-RC2-release-123" to false,
            "2.5.0-dev" to true,
            "2.5.0-dev1" to true,
            "2.5.0-dev-123" to true,
            "2.5.0-SNAPSHOT" to true,
            "2.5.0-public-123" to true,
            "2.5.0-some-playground-123" to true,
        ).forEach { [rawVersion, shouldBeDevVersion] ->
            val parsedVersion = LibrarySpecialCompatibilityChecker.Version.parseVersion(rawVersion)
                ?: fail("Compiler version cannot be parsed: $rawVersion")

            assertEquals(shouldBeDevVersion, parsedVersion.isDevVersion) {
                "Wrong dev version detection result for compiler version $rawVersion"
            }
        }
    }
}
