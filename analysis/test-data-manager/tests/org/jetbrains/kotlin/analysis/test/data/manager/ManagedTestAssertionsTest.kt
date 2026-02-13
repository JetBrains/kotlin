/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.AssertionFailedError
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ManagedTestAssertionsTest {
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        TestDataManagerMode.isUnderTeamCityOverride = false
    }

    @AfterEach
    fun tearDown() {
        TestDataManagerMode.isUnderTeamCityOverride = null
        ManagedTestAssertions.trackUpdatedPaths = false
        ManagedTestAssertions.drainUpdatedTestDataPaths()
    }

    /**
     * Asserts the state of files in tempDir after operation.
     * Format: "filename: content" or "filename: <missing>"
     */
    private fun assertFileState(expected: String) {
        assertFileState(
            fileNames = listOf("test.txt", "test.js.txt", "test.knm.txt", "test.wasm.txt"),
            expected = expected,
        )
    }

    private fun assertFileState(fileNames: List<String>, expected: String) {
        val actual = fileNames.mapNotNull { name ->
            val file = tempDir.resolve(name)
            if (file.exists()) {
                "$name: ${file.readText().trim()}"
            } else {
                null
            }
        }.joinToString("\n")

        assertEquals(expected.trimIndent(), actual)
    }

    private fun setupFiles(vararg files: Pair<String, String>) {
        tempDir.resolve("test.kt").writeText("// test")
        for ((name, content) in files) {
            tempDir.resolve(name).writeText("$content\n")
        }
    }

    private fun runAssertion(
        variantChain: TestVariantChain,
        actual: String,
        mode: TestDataManagerMode = TestDataManagerMode.UPDATE,
        extension: String = ".txt",
    ) {
        ManagedTestAssertions.assertEqualsToTestDataFile(
            testDataPath = tempDir.resolve("test.kt"),
            actual = actual,
            variantChain = variantChain,
            extension = extension,
            mode = mode,
        )
    }

    // ========== UPDATE mode tests ==========

    @Test
    fun `UPDATE mode - golden creates file silently`() {
        setupFiles()  // No expected files

        runAssertion(variantChain = emptyList(), actual = "new content", mode = TestDataManagerMode.UPDATE)

        assertFileState("test.txt: new content")
        // No exception thrown
    }

    @Test
    fun `UPDATE mode - mismatch updates file silently`() {
        setupFiles("test.txt" to "old")

        runAssertion(variantChain = emptyList(), actual = "new", mode = TestDataManagerMode.UPDATE)

        assertFileState("test.txt: new")
        // No exception thrown
    }

    @Test
    fun `UPDATE mode - redundant deletes silently`() {
        setupFiles(
            "test.txt" to "same",
            "test.js.txt" to "same"
        )

        runAssertion(variantChain = listOf("js"), actual = "same", mode = TestDataManagerMode.UPDATE)

        assertFileState("test.txt: same")  // js.txt deleted
        // No exception thrown
    }

    @Test
    fun `UPDATE mode - secondary test fails when no file exists`() {
        setupFiles()  // No expected files

        val ex = assertThrows<IllegalStateException> {
            runAssertion(variantChain = listOf("js"), actual = "content", mode = TestDataManagerMode.UPDATE)
        }

        assertTrue(ex.message!!.contains("No expected file found"))
        assertTrue(ex.message!!.contains("[js]"))
    }

    // ========== CHECK mode tests ==========

    @Test
    fun `CHECK mode - golden missing creates and throws`() {
        setupFiles()  // No expected files

        val ex = assertThrows<AssertionFailedError> {
            runAssertion(variantChain = emptyList(), actual = "content", mode = TestDataManagerMode.CHECK)
        }

        assertFileState("test.txt: content")  // File was created
        assertTrue(ex.message!!.startsWith("Expected data file did not exist, created: "))
    }

    @Test
    fun `CHECK mode - redundant deletes and throws`() {
        setupFiles(
            "test.txt" to "same",
            "test.js.txt" to "same"
        )

        val ex = assertThrows<AssertionFailedError> {
            runAssertion(variantChain = listOf("js"), actual = "same", mode = TestDataManagerMode.CHECK)
        }

        assertFileState("test.txt: same")  // js.txt was deleted
        assertTrue(ex.message!!.contains("Deleted"))
    }

    @Test
    fun `CHECK mode - mismatch throws without updating`() {
        setupFiles("test.txt" to "expected")

        assertThrows<AssertionFailedError> {
            runAssertion(variantChain = emptyList(), actual = "actual", mode = TestDataManagerMode.CHECK)
        }

        assertFileState("test.txt: expected")  // File unchanged
    }

    @Test
    fun `CHECK mode - content matches passes`() {
        setupFiles("test.txt" to "content")

        runAssertion(variantChain = emptyList(), actual = "content", mode = TestDataManagerMode.CHECK)

        assertFileState("test.txt: content")
        // No exception thrown
    }

    @Test
    fun `CHECK mode (TeamCity) - golden missing throws without creating`() {
        TestDataManagerMode.isUnderTeamCityOverride = true
        setupFiles()

        val ex = assertThrows<AssertionFailedError> {
            runAssertion(variantChain = emptyList(), actual = "content", mode = TestDataManagerMode.CHECK)
        }

        assertFileState("") // No file created on TC
        assertTrue(ex.message!!.startsWith("Expected data file did not exist: "))
        assertFalse(ex.message!!.contains("created"))
    }

    @Test
    fun `CHECK mode (TeamCity) - redundant throws without deleting`() {
        TestDataManagerMode.isUnderTeamCityOverride = true
        setupFiles(
            "test.txt" to "same",
            "test.js.txt" to "same"
        )

        val ex = assertThrows<AssertionFailedError> {
            runAssertion(variantChain = listOf("js"), actual = "same", mode = TestDataManagerMode.CHECK)
        }

        assertFileState("test.txt: same\ntest.js.txt: same") // js.txt NOT deleted on TC
        assertTrue(ex.message!!.contains("Delete the prefixed file"))
    }

    @Test
    fun `CHECK mode - secondary test fails when no file exists`() {
        setupFiles()  // No expected files

        val ex = assertThrows<IllegalStateException> {
            runAssertion(variantChain = listOf("js"), actual = "content", mode = TestDataManagerMode.CHECK)
        }

        assertTrue(ex.message!!.contains("No expected file found"))
    }

    // ========== Shared behavior tests ==========

    @Test
    fun `secondary test reads from golden when prefixed missing`() {
        setupFiles("test.txt" to "golden")

        runAssertion(variantChain = listOf("js"), actual = "golden", mode = TestDataManagerMode.UPDATE)

        assertFileState("test.txt: golden")  // No js.txt created
    }

    @Test
    fun `secondary test prefers more specific file`() {
        setupFiles(
            "test.txt" to "golden",
            "test.js.txt" to "js specific"
        )

        runAssertion(variantChain = listOf("js"), actual = "js specific", mode = TestDataManagerMode.UPDATE)

        assertFileState(
            expected = """
                test.txt: golden
                test.js.txt: js specific
            """
        )
    }

    @Test
    fun `multi-prefix reads first existing`() {
        setupFiles(
            "test.txt" to "golden",
            // test.knm.txt missing
            "test.js.txt" to "js"
        )

        // prefixes = [knm, js] → reads: js.txt, knm.txt, txt
        // First existing is js.txt
        runAssertion(variantChain = listOf("knm", "js"), actual = "js", mode = TestDataManagerMode.UPDATE)

        assertFileState(
            expected = """
                test.txt: golden
                test.js.txt: js
            """
        )
    }

    @Test
    fun `keeps write-target when content differs`() {
        setupFiles(
            "test.txt" to "golden",
            "test.js.txt" to "different"
        )

        runAssertion(variantChain = listOf("js"), actual = "different", mode = TestDataManagerMode.UPDATE)

        assertFileState(
            expected = """
                test.txt: golden
                test.js.txt: different
            """
        )
    }

    @Test
    fun `redundancy check skips when write-target missing`() {
        setupFiles("test.txt" to "golden")

        runAssertion(variantChain = listOf("js"), actual = "golden", mode = TestDataManagerMode.UPDATE)

        // No redundancy check needed - js.txt doesn't exist
        assertFileState("test.txt: golden")
    }

    @Test
    fun `mismatch throws AssertionFailedError with FileInfo`() {
        setupFiles("test.txt" to "expected")

        val ex = assertThrows<AssertionFailedError> {
            runAssertion(variantChain = emptyList(), actual = "actual", mode = TestDataManagerMode.CHECK)
        }

        assertNotNull(ex.expected)
        assertTrue(ex.message!!.contains("Actual data differs from file content"))
    }

    @Test
    fun `normalizeContent handles various line endings`() {
        val inputs = listOf(
            "  line1\r\nline2  \r\n  " to "line1\nline2\n",
            "content" to "content\n",
            "  trimmed  " to "trimmed\n",
            "line1\nline2\n" to "line1\nline2\n",
        )

        for ((input, expected) in inputs) {
            assertEquals(
                expected,
                ManagedTestAssertions.normalizeContent(input),
                "Input: ${input.replace("\r", "\\r").replace("\n", "\\n")}"
            )
        }
    }

    // ========== Compound extension tests (.pretty.txt) ==========

    // --- UPDATE mode with compound extension ---

    @Test
    fun `UPDATE mode - compound extension - golden creates file`() {
        setupFiles()  // No expected files

        runAssertion(variantChain = emptyList(), actual = "content", extension = ".pretty.txt")

        assertFileState(
            fileNames = listOf("test.pretty.txt"),
            expected = "test.pretty.txt: content"
        )
    }

    @Test
    fun `UPDATE mode - compound extension - mismatch updates file`() {
        setupFiles("test.pretty.txt" to "old")

        runAssertion(variantChain = emptyList(), actual = "new", extension = ".pretty.txt")

        assertFileState(
            fileNames = listOf("test.pretty.txt"),
            expected = "test.pretty.txt: new"
        )
    }

    @Test
    fun `UPDATE mode - compound extension - redundant deletes`() {
        setupFiles(
            "test.pretty.txt" to "same",
            "test.js.pretty.txt" to "same"
        )

        runAssertion(variantChain = listOf("js"), actual = "same", extension = ".pretty.txt")

        assertFileState(
            fileNames = listOf("test.pretty.txt", "test.js.pretty.txt"),
            expected = "test.pretty.txt: same"  // js.pretty.txt deleted
        )
    }

    // --- CHECK mode with compound extension ---

    @Test
    fun `CHECK mode - compound extension - golden missing creates and throws`() {
        setupFiles()  // No expected files

        val ex = assertThrows<AssertionFailedError> {
            runAssertion(variantChain = emptyList(), actual = "content", extension = ".pretty.txt", mode = TestDataManagerMode.CHECK)
        }

        assertFileState(
            fileNames = listOf("test.pretty.txt"),
            expected = "test.pretty.txt: content"
        )
        assertTrue(ex.message!!.contains("did not exist"))
    }

    @Test
    fun `CHECK mode - compound extension - mismatch throws without updating`() {
        setupFiles("test.pretty.txt" to "expected")

        assertThrows<AssertionFailedError> {
            runAssertion(variantChain = emptyList(), actual = "actual", extension = ".pretty.txt", mode = TestDataManagerMode.CHECK)
        }

        assertFileState(
            fileNames = listOf("test.pretty.txt"),
            expected = "test.pretty.txt: expected"  // File unchanged
        )
    }

    @Test
    fun `CHECK mode - compound extension - redundant deletes and throws`() {
        setupFiles(
            "test.pretty.txt" to "same",
            "test.js.pretty.txt" to "same"
        )

        val ex = assertThrows<AssertionFailedError> {
            runAssertion(variantChain = listOf("js"), actual = "same", extension = ".pretty.txt", mode = TestDataManagerMode.CHECK)
        }

        assertFileState(
            fileNames = listOf("test.pretty.txt", "test.js.pretty.txt"),
            expected = "test.pretty.txt: same"  // js.pretty.txt deleted
        )
        assertTrue(ex.message!!.contains("Deleted"))
    }

    // --- Shared behavior with compound extension ---

    @Test
    fun `compound extension - secondary reads from golden when prefixed missing`() {
        setupFiles("test.pretty.txt" to "golden")

        runAssertion(variantChain = listOf("js"), actual = "golden", extension = ".pretty.txt")

        assertFileState(
            fileNames = listOf("test.pretty.txt", "test.js.pretty.txt"),
            expected = "test.pretty.txt: golden"  // No js.pretty.txt created
        )
    }

    @Test
    fun `compound extension - secondary prefers more specific file`() {
        setupFiles(
            "test.pretty.txt" to "golden",
            "test.js.pretty.txt" to "js specific"
        )

        runAssertion(variantChain = listOf("js"), actual = "js specific", extension = ".pretty.txt")

        assertFileState(
            fileNames = listOf("test.pretty.txt", "test.js.pretty.txt"),
            expected = """
                test.pretty.txt: golden
                test.js.pretty.txt: js specific
            """
        )
    }

    // --- Extension independence ---

    @Test
    fun `different extensions are independent`() {
        setupFiles(
            "test.txt" to "txt content",
            "test.pretty.txt" to "pretty content"
        )

        // Each extension sees only its own file
        runAssertion(variantChain = emptyList(), actual = "txt content", extension = ".txt")
        runAssertion(variantChain = emptyList(), actual = "pretty content", extension = ".pretty.txt")

        assertFileState(
            fileNames = listOf("test.txt", "test.pretty.txt"),
            expected = """
                test.txt: txt content
                test.pretty.txt: pretty content
            """
        )
    }

    @Test
    fun `extension mismatch does not affect other extensions`() {
        setupFiles(
            "test.txt" to "txt",
            "test.pretty.txt" to "pretty"
        )

        // Mismatch on .txt should not affect .pretty.txt
        assertThrows<AssertionFailedError> {
            runAssertion(variantChain = emptyList(), actual = "wrong", extension = ".txt", mode = TestDataManagerMode.CHECK)
        }

        assertFileState(
            fileNames = listOf("test.txt", "test.pretty.txt"),
            expected = """
                test.txt: txt
                test.pretty.txt: pretty
            """
        )
    }

    // ========== Path tracking tests ==========

    @Test
    fun `UPDATE mode - tracking records path on file create`() {
        setupFiles()  // No expected files
        ManagedTestAssertions.trackUpdatedPaths = true

        runAssertion(variantChain = emptyList(), actual = "new content", mode = TestDataManagerMode.UPDATE)

        val paths = ManagedTestAssertions.drainUpdatedTestDataPaths()
        assertEquals(1, paths.size)
        assertTrue(paths.single().endsWith("test.kt"))
    }

    @Test
    fun `UPDATE mode - tracking records path on mismatch update`() {
        setupFiles("test.txt" to "old")
        ManagedTestAssertions.trackUpdatedPaths = true

        runAssertion(variantChain = emptyList(), actual = "new", mode = TestDataManagerMode.UPDATE)

        val paths = ManagedTestAssertions.drainUpdatedTestDataPaths()
        assertEquals(1, paths.size)
        assertTrue(paths.single().endsWith("test.kt"))
    }

    @Test
    fun `UPDATE mode - tracking does not record when disabled`() {
        setupFiles()  // No expected files
        ManagedTestAssertions.trackUpdatedPaths = false

        runAssertion(variantChain = emptyList(), actual = "new content", mode = TestDataManagerMode.UPDATE)

        val paths = ManagedTestAssertions.drainUpdatedTestDataPaths()
        assertTrue(paths.isEmpty())
    }

    @Test
    fun `UPDATE mode - tracking does not record when content matches`() {
        setupFiles("test.txt" to "content")
        ManagedTestAssertions.trackUpdatedPaths = true

        runAssertion(variantChain = emptyList(), actual = "content", mode = TestDataManagerMode.UPDATE)

        val paths = ManagedTestAssertions.drainUpdatedTestDataPaths()
        assertTrue(paths.isEmpty())
    }

    @Test
    fun `drainUpdatedTestDataPaths clears set after drain`() {
        setupFiles()  // No expected files
        ManagedTestAssertions.trackUpdatedPaths = true

        runAssertion(variantChain = emptyList(), actual = "content", mode = TestDataManagerMode.UPDATE)

        val firstDrain = ManagedTestAssertions.drainUpdatedTestDataPaths()
        assertEquals(1, firstDrain.size)

        val secondDrain = ManagedTestAssertions.drainUpdatedTestDataPaths()
        assertTrue(secondDrain.isEmpty())
    }
}
