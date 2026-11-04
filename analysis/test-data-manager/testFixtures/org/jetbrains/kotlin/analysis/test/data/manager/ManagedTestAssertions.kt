/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.handleMissingFile
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.isGoldenTest
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.normalizeContent
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.*

/**
 * Assertions for managed test data files.
 *
 * Provides a single entry point for comparing test outputs with expected files
 * and applying the managed test data behavior matrix.
 */
object ManagedTestAssertions {

    /**
     * The file modifications made while [recordFileActions] is on, in order, before [drainFileActions] collapses them.
     */
    private val recordedFileActions = ConcurrentLinkedQueue<RecordedFileAction>()

    private class RecordedFileAction(val testDataPath: Path, val file: Path, val existedBefore: Boolean, val existsAfter: Boolean)

    /**
     * Runs the modification of [file] in [block], recording it if [recordFileActions] is on.
     *
     * This function must wrap all modification actions
     */
    context(context: TestDataContext)
    private inline fun modify(file: Path, block: () -> Unit) {
        val existedBefore = file.exists()
        block()
        if (recordFileActions) {
            recordedFileActions.add(RecordedFileAction(context.testDataPath, file, existedBefore, file.exists()))
        }
    }

    /**
     * Whether the current environment allows file modifications (create, update, delete).
     *
     * UPDATE always modifies. CHECK is read-only on TeamCity and when requested explicitly (`checkTestData`);
     * only plain local test runs, where the mode is not set, keep creating and deleting files in CHECK.
     *
     * This is an environment-level gate. Individual handlers may impose additional conditions
     * (e.g., [handleMissingFile] also requires [isGoldenTest] in CHECK mode).
     */
    context(context: TestDataContext)
    private val isModificationAllowed: Boolean
        get() = context.mode == TestDataManagerMode.UPDATE ||
                !(TestDataManagerMode.isUnderTeamCity || TestDataManagerMode.isExplicitCheck)


    /** Whether this is a golden (base) test with no variant chain. */
    context(context: TestDataContext)
    private val isGoldenTest: Boolean
        get() = context.variantChain.isEmpty()

    /**
     * When true, every file creation, update and deletion is recorded for [drainFileActions].
     * Set by the test data manager runner: plain test runs don't collect them.
     */
    @Volatile
    internal var recordFileActions: Boolean = false

    /**
     * Returns the files changed since the previous call, one [FileAction] per file with its net effect:
     * an update followed by a deletion is a deletion, a creation followed by a deletion is nothing.
     */
    internal fun drainFileActions(): List<FileAction> {
        val recorded = generateSequence { recordedFileActions.poll() }.toList()
        return recorded.groupBy { it.file }.mapNotNull { [file, actions] ->
            val kind = FileAction.Kind.of(actions.first().existedBefore, actions.last().existsAfter) ?: return@mapNotNull null
            FileAction(actions.first().testDataPath, file, kind)
        }
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
     * | Scenario                  | UPDATE | CHECK (local test run) | CHECK (`checkTestData`, TeamCity) |
     * |---------------------------|--------|------------------------|-----------------------------------|
     * | actual=null, file missing | Pass   | Pass                   | Pass                              |
     * | actual=null, file exists  | Delete | Delete + throw         | Throw                             |
     * | File missing (golden)     | Create | Create + throw         | Throw                             |
     * | File missing (secondary)  | Create | Throw                  | Throw                             |
     * | Content matches           | Pass   | Pass                   | Pass                              |
     * | Write-target redundant    | Delete | Delete + throw         | Throw                             |
     * | Content mismatch          | Update | Throw                  | Throw                             |
     *
     * "Local test run" is a test started without the mode set (plain `test` task, IDE gutter): CHECK is its fallback.
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
    ): Unit =
        assertEqualsToTestDataFile(actual, context = TestDataContext.build(testDataPath, variantChain, extension, mode, sanitizer))

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
            handleMismatch(normalizedActual, expectedFile, expectedContent)
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
            if (isModificationAllowed) modify(writeTargetFile) {
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
    private fun handleMismatch(normalizedActual: String, expectedFile: Path, expectedContent: String) {
        val contentToWrite = preserveEofNewline(normalizedActual, expectedContent)
        when (context.mode) {
            TestDataManagerMode.UPDATE -> writeFile(contentToWrite)
            TestDataManagerMode.CHECK -> throw createMismatchAssertion(
                expectedFile = expectedFile,
                expectedContent = expectedContent,
                normalizedActual = contentToWrite,
            )
        }
    }

    context(context: TestDataContext)
    private fun handleFileShouldNotExist() {
        val writeTargetFile = context.writeTargetFile
        if (!writeTargetFile.exists()) return

        if (isModificationAllowed) modify(writeTargetFile) {
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

        return TestDataAssertionError(message, TestDataAssertionError.Kind.UNEXPECTED_FILE, writeTargetFile, comparedFile = null)
    }

    context(context: TestDataContext)
    private fun writeFile(content: String) {
        val path = context.writeTargetFile
        modify(path) {
            path.createParentDirectories()
            path.writeText(content)
        }
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

        return TestDataAssertionError(
            message,
            TestDataAssertionError.Kind.MISSING_FILE,
            writeTargetFile,
            comparedFile = null,
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

        return TestDataAssertionError(
            prefix + postfix,
            TestDataAssertionError.Kind.REDUNDANT_FILE,
            writeTargetFile,
            comparedFile = nextExistingFile,
        )
    }

    context(context: TestDataContext)
    private fun createMismatchAssertion(
        expectedFile: Path,
        expectedContent: String,
        normalizedActual: String,
    ): AssertionFailedError {
        val writeTargetFile = context.writeTargetFile
        return TestDataAssertionError(
            "Actual data differs from file content: ${writeTargetFile.name}",
            TestDataAssertionError.Kind.MISMATCH,
            writeTargetFile,
            comparedFile = expectedFile,
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
