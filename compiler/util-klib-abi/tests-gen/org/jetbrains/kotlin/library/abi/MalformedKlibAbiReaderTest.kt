/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalLibraryAbiReader
class MalformedKlibAbiReaderTest {
    private lateinit var testDataDir: File

    @BeforeEach
    fun setUp() {
        testDataDir = File("compiler/testData/klib/dump-abi/malformed").absoluteFile
        check(testDataDir.isDirectory) { "Test data dir does not exist: $testDataDir" }
    }

    @Test
    fun testKlibDoesNotExist() {
        val nonExistingLibraries = listOf(
            "nonExistingLibrary", "nonExistingLibrary.klib"
        ).map(testDataDir::resolve)

        nonExistingLibraries.forEach { nonExistingLibrary ->
            assertInvalidKlib(nonExistingLibrary)
        }
    }

    @Test
    fun testMalformedKlib() {
        val malformedLibraries = listOf(
            "malformed-klib1.klib",
            "malformed-klib2",
            "malformed-klib3",
            "malformed-klib4",
        ).map(testDataDir::resolve)

        malformedLibraries.forEach { malformedLibrary ->
            assertTrue(malformedLibrary.exists()) { "Library does not exist, check the test data: $malformedLibrary" }
            assertInvalidKlib(malformedLibrary)
        }
    }

    private fun assertInvalidKlib(libraryFile: File) {
        try {
            LibraryAbiReader.readAbiInfo(libraryFile)
            fail { "Unexpectedly successful read from invalid library $libraryFile" }
        } catch (e: Exception) {
            val exceptionMessage = e.message.orEmpty()
            assertTrue(libraryFile.path in exceptionMessage) {
                """
                Exception message does not contain library path.
                Message: $exceptionMessage
                Library file: $libraryFile
                """.trimIndent()
            }
        }
    }
}
