/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions.trackUpdatedPaths
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
    private inline fun TestDataFiles.update(block: () -> Unit) {
        if (trackUpdatedPaths) {
            updatedTestDataPaths.add(testDataPath.toString())
        }

        block()
    }

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
     * | File missing (golden)    | Create        | Create + throw  | Throw            |
     * | File missing (secondary) | Create        | Throw           | Throw            |
     * | Content matches          | Pass          | Pass            | Pass             |
     * | Write-target redundant   | Delete        | Delete + throw  | Throw            |
     * | Content mismatch         | Update        | Throw           | Throw            |
     *
     * @param testDataPath path to the test input file (used to derive expected file paths)
     * @param actual actual content to compare
     * @param variantChain chain of variant identifiers from [ManagedTest.variantChain]
     * @param extension file extension for expected files (e.g., ".txt")
     * @param mode test data manager mode (defaults to [TestDataManagerMode.currentMode])
     * @throws AssertionFailedError on mismatch, missing file, or redundant file
     */
    fun assertEqualsToTestDataFile(
        testDataPath: Path,
        actual: String,
        variantChain: TestVariantChain,
        extension: String,
        mode: TestDataManagerMode = TestDataManagerMode.currentMode,
    ) {
        val isGoldenTest = variantChain.isEmpty()
        val testDataFiles = TestDataFiles.build(testDataPath, variantChain, extension)

        val normalizedActual = normalizeContent(actual)

        // Find first existing file to compare against
        val expectedFile = testDataFiles.readableFiles.firstOrNull { it.exists() }

        // Handle missing expected file
        if (expectedFile == null) {
            handleMissingFile(
                isGoldenTest = isGoldenTest,
                testDataFiles = testDataFiles,
                normalizedActual = normalizedActual,
                mode = mode,
                variantChain = variantChain,
            )

            return
        }

        val expectedContent = expectedFile.readText()
        val normalizedExpected = normalizeContent(expectedContent)

        if (normalizedActual != normalizedExpected) {
            handleMismatch(
                testDataFiles = testDataFiles,
                normalizedActual = normalizedActual,
                expectedContent = expectedContent,
                mode = mode,
            )
        }

        checkAndHandleRedundantFile(testDataFiles, mode)
    }

    private fun handleMissingFile(
        isGoldenTest: Boolean,
        testDataFiles: TestDataFiles,
        normalizedActual: String,
        mode: TestDataManagerMode,
        variantChain: TestVariantChain,
    ) {
        when (mode) {
            TestDataManagerMode.UPDATE -> writeFile(
                testDataFiles = testDataFiles,
                content = normalizedActual,
            )

            TestDataManagerMode.CHECK -> {
                val modificationAllowed = isGoldenTest && !TestDataManagerMode.isUnderTeamCity
                if (modificationAllowed) writeFile(
                    testDataFiles = testDataFiles,
                    content = normalizedActual,
                )

                throw createMissingFileAssertion(
                    testDataFiles = testDataFiles,
                    normalizedActual = normalizedActual,
                    isGoldenTest = isGoldenTest,
                    variantChain = variantChain,
                    fileWasCreated = modificationAllowed,
                )
            }
        }
    }

    private fun checkAndHandleRedundantFile(testDataFiles: TestDataFiles, mode: TestDataManagerMode) {
        val writeTargetFile = testDataFiles.writeTargetFile
        if (!writeTargetFile.exists()) return

        // Find the next existing file after writeTargetFile (less specific)
        val nextExistingFile = testDataFiles.firstNonWritableFileIfExists ?: return

        val writeTargetContent = normalizeContent(writeTargetFile.readText())
        val nextContent = normalizeContent(nextExistingFile.readText())

        if (writeTargetContent == nextContent) {
            val modificationAllowed = mode == TestDataManagerMode.UPDATE || !TestDataManagerMode.isUnderTeamCity
            if (modificationAllowed) testDataFiles.update {
                writeTargetFile.deleteIfExists()
            }

            if (mode == TestDataManagerMode.CHECK) {
                throw createRedundantFileAssertion(
                    writeTargetFile = writeTargetFile,
                    nextExistingFile = nextExistingFile,
                    fileWasDeleted = modificationAllowed,
                )
            }
        }
    }

    private fun handleMismatch(
        testDataFiles: TestDataFiles,
        normalizedActual: String,
        expectedContent: String,
        mode: TestDataManagerMode,
    ) {
        val contentToWrite = preserveEofNewline(normalizedActual, expectedContent)
        when (mode) {
            TestDataManagerMode.UPDATE -> writeFile(
                testDataFiles = testDataFiles,
                content = contentToWrite,
            )

            TestDataManagerMode.CHECK -> throw createMismatchAssertion(
                testDataFiles = testDataFiles,
                expectedContent = expectedContent,
                normalizedActual = contentToWrite,
            )
        }
    }

    private fun writeFile(testDataFiles: TestDataFiles, content: String): Unit = testDataFiles.update {
        val path = testDataFiles.writeTargetFile
        path.createParentDirectories()
        path.writeText(content)
    }

    private fun createMissingFileAssertion(
        testDataFiles: TestDataFiles,
        normalizedActual: String,
        isGoldenTest: Boolean,
        variantChain: TestVariantChain,
        fileWasCreated: Boolean,
    ): AssertionFailedError {
        val writeTargetFile = testDataFiles.writeTargetFile
        val message = if (isGoldenTest) {
            "Expected data file did not exist${if (fileWasCreated) ", created" else ""}: $writeTargetFile"
        } else {
            "No expected file found for secondary test with variant chain $variantChain. " +
                    "Searched: ${testDataFiles.readableFiles.joinToString { it.name }}. " +
                    "Run golden test first to generate base file, or run 'manageTestDataGlobally --mode=update' to auto-generate."
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

    private fun createMismatchAssertion(
        testDataFiles: TestDataFiles,
        expectedContent: String,
        normalizedActual: String,
    ): AssertionFailedError {
        val writeTargetFile = testDataFiles.writeTargetFile
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

    internal fun normalizeContent(content: String): String =
        content.trim().convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
}

/**
 * Extension for convenient usage from [ManagedTest] implementations.
 */
fun ManagedTest.assertEqualsToTestDataFile(
    testDataPath: Path,
    actual: String,
    extension: String,
) {
    ManagedTestAssertions.assertEqualsToTestDataFile(
        testDataPath = testDataPath,
        actual = actual,
        variantChain = variantChain,
        extension = extension,
    )
}
