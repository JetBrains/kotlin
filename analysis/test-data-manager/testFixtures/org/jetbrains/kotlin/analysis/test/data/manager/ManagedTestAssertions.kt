/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.handleMissingFile
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.isGoldenTest
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.normalizeContent
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.updatedTestDataPaths
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*

/**
 * Assertions for managed test data files.
 *
 * Provides a single entry point for comparing test outputs with expected files
 * and applying the managed test data behavior matrix.
 */
object ManagedTestAssertions {

    /**
     * Thread-safe set of test data paths (absolute path strings) that were updated during the current run.
     * Used by incremental mode to determine which variant tests to run.
     */
    private val updatedTestDataPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Auto-updates the [updatedTestDataPaths] set if [trackUpdatedPaths] is true.
     *
     * This function must wrap all modification actions
     */
    context(context: TestDataContext)
    private inline fun update(block: () -> Unit) {
        if (trackUpdatedPaths) {
            updatedTestDataPaths.add(context.testDataPath.toString())
        }

        block()
    }

    /**
     * Whether the current environment allows file modifications (create, update, delete).
     *
     * This is an environment-level gate. Individual handlers may impose additional conditions
     * (e.g., [handleMissingFile] also requires [isGoldenTest] in CHECK mode).
     */
    context(context: TestDataContext)
    private val isModificationAllowed: Boolean
        get() = context.mode == TestDataManagerMode.UPDATE || !TestDataManagerMode.isUnderTeamCity


    /** Whether this is a golden (base) test with no variant chain. */
    context(context: TestDataContext)
    private val isGoldenTest: Boolean
        get() = context.variantChain.isEmpty()

    /**
     * When true, file writes in UPDATE mode will record the test data path in [updatedTestDataPaths].
     */
    @Volatile
    internal var trackUpdatedPaths: Boolean = false

    /**
     * Returns the set of updated test data paths collected so far and clears the internal set.
     */
    internal fun drainUpdatedTestDataPaths(): Set<String> {
        val snapshot = updatedTestDataPaths.toSet()
        updatedTestDataPaths.clear()
        return snapshot
    }

    /**
     * Compares actual content with the expected test output file.
     *
     * File resolution follows variant chain priority (most specific first):
     * - For `["knm", "js"]`: tries `.js.txt` → `.knm.txt` → `.txt`
     * - For `[]`: tries `.txt` only
     *
     * ## Behavior Matrix
     *
     * | Scenario                 | UPDATE        | CHECK (local)   | CHECK (TeamCity) |
     * |--------------------------|---------------|-----------------|------------------|
     * | actual=null, file missing | Pass         | Pass            | Pass             |
     * | actual=null, file exists  | Delete       | Delete + throw  | Throw            |
     * | File missing (golden)    | Create        | Create + throw  | Throw            |
     * | File missing (secondary) | Create        | Throw           | Throw            |
     * | Content matches          | Pass          | Pass            | Pass             |
     * | Write-target redundant   | Delete        | Delete + throw  | Throw            |
     * | Content mismatch         | Update        | Throw           | Throw            |
     *
     * @param testDataPath path to the test input file (used to derive expected file paths)
     * @param actual actual content to compare, or `null` if the file should not exist
     * @param variantChain chain of variant identifiers from [ManagedTest.variantChain]
     * @param extension file extension for expected files (e.g., ".txt")
     * @param mode test data manager mode (defaults to [TestDataManagerMode.currentMode])
     * @param sanitizer optional sanitizer, applied to both actual and expected content.
     * @throws AssertionFailedError on mismatch, missing file, or redundant file
     */
    fun assertEqualsToTestDataFile(
        testDataPath: Path,
        actual: String?,
        variantChain: TestVariantChain,
        extension: String,
        mode: TestDataManagerMode = TestDataManagerMode.currentMode,
        sanitizer: (String) -> String = { it },
    ): Unit = context(TestDataContext.build(testDataPath, variantChain, extension, mode, sanitizer)) {
        assertEqualsToTestDataFile(actual)
    }

    context(context: TestDataContext)
    private fun assertEqualsToTestDataFile(actual: String?) {
        if (actual == null) {
            handleFileShouldNotExist()
            return
        }

        val normalizedActual = normalizeContent(actual)

        // Find first existing file to compare against
        val expectedFile = context.readTargetFile

        // Handle missing expected file
        if (expectedFile == null) {
            handleMissingFile(normalizedActual)
            return
        }

        val expectedContent = expectedFile.readText()
        val normalizedExpected = normalizeContent(expectedContent)

        if (normalizedActual != normalizedExpected) {
            handleMismatch(normalizedActual, expectedContent)
        }

        checkAndHandleRedundantFile()
    }

    context(context: TestDataContext)
    private fun handleMissingFile(normalizedActual: String) {
        val modificationAllowed = isModificationAllowed && (isGoldenTest || context.mode == TestDataManagerMode.UPDATE)
        if (modificationAllowed) {
            writeFile(normalizedActual)
        }

        if (context.mode == TestDataManagerMode.CHECK) {
            throw createMissingFileAssertion(
                normalizedActual = normalizedActual,
                fileWasCreated = modificationAllowed,
            )
        }
    }

    context(context: TestDataContext)
    private fun checkAndHandleRedundantFile() {
        val writeTargetFile = context.writeTargetFile
        if (!writeTargetFile.exists()) return

        // Find the next existing file after writeTargetFile (less specific)
        val nextExistingFile = context.fallbackFile ?: return

        val writeTargetContent = normalizeContent(writeTargetFile.readText())
        val nextContent = normalizeContent(nextExistingFile.readText())

        if (writeTargetContent == nextContent) {
            if (isModificationAllowed) update {
                writeTargetFile.deleteIfExists()
            }

            if (context.mode == TestDataManagerMode.CHECK) {
                throw createRedundantFileAssertion(
                    writeTargetFile = writeTargetFile,
                    nextExistingFile = nextExistingFile,
                    fileWasDeleted = isModificationAllowed,
                )
            }
        }
    }

    context(context: TestDataContext)
    private fun handleMismatch(normalizedActual: String, expectedContent: String) {
        val contentToWrite = preserveEofNewline(normalizedActual, expectedContent)
        when (context.mode) {
            TestDataManagerMode.UPDATE -> writeFile(contentToWrite)
            TestDataManagerMode.CHECK -> throw createMismatchAssertion(
                expectedContent = expectedContent,
                normalizedActual = contentToWrite,
            )
        }
    }

    context(context: TestDataContext)
    private fun handleFileShouldNotExist() {
        val writeTargetFile = context.writeTargetFile
        if (!writeTargetFile.exists()) return

        if (isModificationAllowed) update {
            writeTargetFile.deleteIfExists()
        }

        if (context.mode == TestDataManagerMode.CHECK) {
            throw createUnexpectedFileAssertion(
                writeTargetFile = writeTargetFile,
                fileWasDeleted = isModificationAllowed,
            )
        }
    }

    private fun createUnexpectedFileAssertion(writeTargetFile: Path, fileWasDeleted: Boolean): AssertionFailedError {
        val message = buildString {
            append("File should not exist")
            if (fileWasDeleted) append(", deleted")
            append(": ")
            append(writeTargetFile)
        }

        return AssertionFailedError(message)
    }

    context(context: TestDataContext)
    private fun writeFile(content: String): Unit = update {
        val path = context.writeTargetFile
        path.createParentDirectories()
        path.writeText(content)
    }

    context(context: TestDataContext)
    private fun createMissingFileAssertion(
        normalizedActual: String,
        fileWasCreated: Boolean,
    ): AssertionFailedError {
        val writeTargetFile = context.writeTargetFile
        val message = if (isGoldenTest) {
            "Expected data file did not exist${if (fileWasCreated) ", created" else ""}: $writeTargetFile"
        } else {
            "No expected file found for secondary test with variant chain ${context.variantChain}. " +
                    "Searched: ${context.readableFiles.joinToString { it.name }}. " +
                    "Run golden test first to generate base file, or run 'updateTestData' to auto-generate."
        }

        return AssertionFailedError(
            message,
            FileInfo(writeTargetFile.absolutePathString(), byteArrayOf()),
            normalizedActual,
        )
    }

    private fun createRedundantFileAssertion(
        writeTargetFile: Path,
        nextExistingFile: Path,
        fileWasDeleted: Boolean,
    ): AssertionFailedError {
        val prefix = "\"${writeTargetFile.name}\" had the same content as \"${nextExistingFile.name}\". "
        val postfix = if (fileWasDeleted) {
            "Deleted the redundant prefixed file."
        } else {
            "Delete the prefixed file."
        }

        return AssertionFailedError(prefix + postfix)
    }

    context(context: TestDataContext)
    private fun createMismatchAssertion(
        expectedContent: String,
        normalizedActual: String,
    ): AssertionFailedError {
        val writeTargetFile = context.writeTargetFile
        return AssertionFailedError(
            "Actual data differs from file content: ${writeTargetFile.name}",
            FileInfo(
                writeTargetFile.absolutePathString(),
                expectedContent.toByteArray(StandardCharsets.UTF_8),
            ),
            normalizedActual,
        )
    }

    /**
     * Adjusts the trailing newline in [normalizedContent] to match the EOF status of [existingContent].
     *
     * [normalizedContent] always ends with `\n` (from [normalizeContent]).
     * If the existing file had no trailing newline, the trailing `\n` is stripped before writing,
     * so the update doesn't introduce an unwanted EOF change.
     */
    private fun preserveEofNewline(normalizedContent: String, existingContent: String): String =
        if (!existingContent.endsWith("\n")) normalizedContent.trimEnd('\n') else normalizedContent

    context(context: TestDataContext)
    private fun normalizeContent(content: String): String = normalizeContent(content, context.sanitizer)

    internal fun normalizeContent(content: String, sanitizer: (String) -> String): String =
        content.trim().convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF().let(sanitizer)
}
