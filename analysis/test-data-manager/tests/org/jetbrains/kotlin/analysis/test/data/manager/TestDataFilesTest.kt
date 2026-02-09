/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TestDataFilesTest {
    @TempDir
    lateinit var tempDir: Path

    private fun assertTestDataFiles(
        prefixes: List<String>,
        expectedReadable: String,
        expectedWriteTarget: String,
        extension: String = ".txt",
    ) {
        val testDataPath = tempDir.resolve("test.kt")
        val testDataFiles = TestDataFiles.build(testDataPath, prefixes, extension)
        val actualReadable = testDataFiles.readableFiles.joinToString(", ") { it.fileName.toString() }
        assertEquals(expectedReadable, actualReadable)
        assertEquals(expectedWriteTarget, testDataFiles.writeTargetFile.fileName.toString())
    }

    @Test
    fun `golden only`() {
        assertTestDataFiles(
            prefixes = emptyList(),
            expectedReadable = "test.txt",
            expectedWriteTarget = "test.txt",
        )
    }

    @Test
    fun `single prefix`() {
        assertTestDataFiles(
            prefixes = listOf("js"),
            expectedReadable = "test.js.txt, test.txt",
            expectedWriteTarget = "test.js.txt",
        )
    }

    @Test
    fun `multiple prefixes`() {
        assertTestDataFiles(
            prefixes = listOf("knm", "js"),
            expectedReadable = "test.js.txt, test.knm.txt, test.txt",
            expectedWriteTarget = "test.js.txt",
        )
    }

    // ========== Compound extension tests ==========

    @Test
    fun `compound extension - golden only`() {
        assertTestDataFiles(
            prefixes = emptyList(),
            extension = ".pretty.txt",
            expectedReadable = "test.pretty.txt",
            expectedWriteTarget = "test.pretty.txt",
        )
    }

    @Test
    fun `compound extension - single prefix`() {
        assertTestDataFiles(
            prefixes = listOf("js"),
            extension = ".pretty.txt",
            expectedReadable = "test.js.pretty.txt, test.pretty.txt",
            expectedWriteTarget = "test.js.pretty.txt",
        )
    }

    @Test
    fun `compound extension - multiple prefixes`() {
        assertTestDataFiles(
            prefixes = listOf("knm", "js"),
            extension = ".pretty.txt",
            expectedReadable = "test.js.pretty.txt, test.knm.pretty.txt, test.pretty.txt",
            expectedWriteTarget = "test.js.pretty.txt",
        )
    }
}
