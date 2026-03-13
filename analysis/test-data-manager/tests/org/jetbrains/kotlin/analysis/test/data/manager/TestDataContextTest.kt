/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TestDataContextTest {
    @TempDir
    lateinit var tempDir: Path

    private fun assertTestDataContext(
        prefixes: List<String>,
        expectedReadable: String,
        expectedWriteTarget: String,
        extension: String = ".txt",
    ) {
        val testDataPath = tempDir.resolve("test.kt")
        val context = TestDataContext.build(testDataPath, prefixes, extension, TestDataManagerMode.CHECK)
        val actualReadable = context.readableFiles.joinToString(", ") { it.fileName.toString() }
        assertEquals(expectedReadable, actualReadable)
        assertEquals(expectedWriteTarget, context.writeTargetFile.fileName.toString())
    }

    @Test
    fun `golden only`() {
        assertTestDataContext(
            prefixes = emptyList(),
            expectedReadable = "test.txt",
            expectedWriteTarget = "test.txt",
        )
    }

    @Test
    fun `single prefix`() {
        assertTestDataContext(
            prefixes = listOf("js"),
            expectedReadable = "test.js.txt, test.txt",
            expectedWriteTarget = "test.js.txt",
        )
    }

    @Test
    fun `multiple prefixes`() {
        assertTestDataContext(
            prefixes = listOf("knm", "js"),
            expectedReadable = "test.js.txt, test.knm.txt, test.txt",
            expectedWriteTarget = "test.js.txt",
        )
    }

    // ========== Compound extension tests ==========

    @Test
    fun `compound extension - golden only`() {
        assertTestDataContext(
            prefixes = emptyList(),
            extension = ".pretty.txt",
            expectedReadable = "test.pretty.txt",
            expectedWriteTarget = "test.pretty.txt",
        )
    }

    @Test
    fun `compound extension - single prefix`() {
        assertTestDataContext(
            prefixes = listOf("js"),
            extension = ".pretty.txt",
            expectedReadable = "test.js.pretty.txt, test.pretty.txt",
            expectedWriteTarget = "test.js.pretty.txt",
        )
    }

    @Test
    fun `compound extension - multiple prefixes`() {
        assertTestDataContext(
            prefixes = listOf("knm", "js"),
            extension = ".pretty.txt",
            expectedReadable = "test.js.pretty.txt, test.knm.pretty.txt, test.pretty.txt",
            expectedWriteTarget = "test.js.pretty.txt",
        )
    }
}
