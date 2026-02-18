/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

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
 * Provides a single entry point for comparing test outputs with expected files,
 * enforcing the golden-only auto-generation constraint.
 */
object ManagedTestAssertions {

    /**
     * Thread-safe set of test data paths (absolute path strings) that were updated during the current run.
     * Used by incremental mode to determine which variant tests to run.
     */
    private val updatedTestDataPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

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
     * | File missing (secondary) | Throw         | Throw           | Throw            |
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
     * @throws IllegalStateException if no readable file found for secondary test
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
                testDataPath = testDataPath,
            )

            return
        }

        val expectedContent = expectedFile.readText()
        val normalizedExpected = normalizeContent(expectedContent)

        // Content comparison
        if (normalizedActual == normalizedExpected) {
            // Check and handle redundant write-target file
            checkAndHandleRedundantFile(testDataFiles, mode)
            return
        }

        // Content mismatch
        handleMismatch(
            testDataFiles = testDataFiles,
            normalizedActual = normalizedActual,
            expectedContent = expectedContent,
            mode = mode,
            testDataPath = testDataPath,
        )
    }

    private fun handleMissingFile(
        isGoldenTest: Boolean,
        testDataFiles: TestDataFiles,
        normalizedActual: String,
        mode: TestDataManagerMode,
        variantChain: TestVariantChain,
        testDataPath: Path,
    ) {
        val writeTargetFile = testDataFiles.writeTargetFile

        if (isGoldenTest) {
            when (mode) {
                TestDataManagerMode.UPDATE -> {
                    writeTargetFile.createParentDirectories()
                    writeTargetFile.writeText(normalizedActual)
                    if (trackUpdatedPaths) updatedTestDataPaths.add(testDataPath.toString())
                }

                TestDataManagerMode.CHECK -> {
                    if (TestDataManagerMode.isUnderTeamCity) {
                        throw AssertionFailedError(
                            "Expected data file did not exist: $writeTargetFile",
                            FileInfo(writeTargetFile.absolutePathString(), byteArrayOf()),
                            normalizedActual,
                        )
                    } else {
                        writeTargetFile.createParentDirectories()
                        writeTargetFile.writeText(normalizedActual)
                        throw AssertionFailedError(
                            "Expected data file did not exist, created: $writeTargetFile",
                            FileInfo(writeTargetFile.absolutePathString(), byteArrayOf()),
                            normalizedActual,
                        )
                    }
                }
            }
        } else {
            // Secondary test: always fail
            throw IllegalStateException(
                "No expected file found for secondary test with variant chain $variantChain. " +
                        "Searched: ${testDataFiles.readableFiles.joinToString { it.name }}. " +
                        "Run golden test first to generate base file."
            )
        }
    }

    private fun checkAndHandleRedundantFile(
        testDataFiles: TestDataFiles,
        mode: TestDataManagerMode,
    ) {
        val writeTargetFile = testDataFiles.writeTargetFile
        if (!writeTargetFile.exists()) return

        // Find the next existing file after writeTargetFile (less specific)
        val nextExistingFile = testDataFiles.firstNonWritableFileIfExists ?: return

        val writeTargetContent = normalizeContent(writeTargetFile.readText())
        val nextContent = normalizeContent(nextExistingFile.readText())

        if (writeTargetContent == nextContent) {
            when (mode) {
                TestDataManagerMode.UPDATE -> {
                    writeTargetFile.deleteIfExists()
                }

                TestDataManagerMode.CHECK -> {
                    if (TestDataManagerMode.isUnderTeamCity) {
                        throw AssertionFailedError(
                            "\"${writeTargetFile.name}\" has the same content as \"${nextExistingFile.name}\". " +
                                    "Delete the prefixed file."
                        )
                    } else {
                        writeTargetFile.deleteIfExists()
                        throw AssertionFailedError(
                            "\"${writeTargetFile.name}\" had the same content as \"${nextExistingFile.name}\". " +
                                    "Deleted the redundant prefixed file."
                        )
                    }
                }
            }
        }
    }

    private fun handleMismatch(
        testDataFiles: TestDataFiles,
        normalizedActual: String,
        expectedContent: String,
        mode: TestDataManagerMode,
        testDataPath: Path,
    ) {
        val writeTargetFile = testDataFiles.writeTargetFile

        when (mode) {
            TestDataManagerMode.UPDATE -> {
                writeTargetFile.createParentDirectories()
                writeTargetFile.writeText(normalizedActual)
                if (trackUpdatedPaths) updatedTestDataPaths.add(testDataPath.toString())
            }
            TestDataManagerMode.CHECK -> {
                throw AssertionFailedError(
                    "Actual data differs from file content: ${writeTargetFile.name}",
                    FileInfo(
                        writeTargetFile.absolutePathString(),
                        expectedContent.toByteArray(StandardCharsets.UTF_8),
                    ),
                    normalizedActual,
                )
            }
        }
    }

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
