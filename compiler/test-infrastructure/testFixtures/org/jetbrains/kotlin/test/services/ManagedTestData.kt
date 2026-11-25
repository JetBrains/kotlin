/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerMode
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.utils.extensionWithDot
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import java.io.File
import java.nio.file.Path

/**
 * Lets `checkTestData` and `updateTestData` manage the test data with inline diagnostics of a test implementing [ManagedTest], and its
 * dumps (see [assertEqualsToDump]).
 *
 * A variant of the test data is the base test data file with the variant before its extension, e.g., `foo.ll.kt` for the `ll`
 * variant of `foo.kt`. Such a file is both the input of the test and its expected output: [ManagedTestDataConfigurator] makes the
 * test read the expected file of its [variantChain], and [assertEqualsToTestDataFile] compares the actual output with the same file
 * via [ManagedTestAssertions].
 *
 * The variants of the compiler test data are the constants next to [LL_FIR_VARIANT][org.jetbrains.kotlin.test.utils.LL_FIR_VARIANT], and
 * the variant chains of its tests are in `DiagnosticsVariantChains` and `LLDiagnosticsVariantChains`.
 */
class ManagedTestData(private val testServices: TestServices, val variantChain: TestVariantChain) : TestService {
    /**
     * The test data file without a variant, which [ManagedTestDataConfigurator] replaces with the expected file.
     */
    lateinit var baseTestDataFile: File
        private set

    /**
     * Whether the test data is updated rather than checked. A test which expects failures is checked even by `updateTestData` (see
     * [mode]), and so is a test whose inline diagnostics were incomplete or failed to be written (see [assertEqualsToTestDataFile]).
     */
    internal val updatesTestData: Boolean
        get() = mode == TestDataManagerMode.UPDATE && isInlineComparisonSuccessful != false

    /**
     * The mode of the comparison. A test which expects failures (see
     * [TestFailureSuppressor.expectsFailures][org.jetbrains.kotlin.test.model.TestFailureSuppressor.expectsFailures]) is compared in
     * [TestDataManagerMode.CHECK] mode even by `updateTestData`, as its output may be wrong.
     */
    private val mode: TestDataManagerMode
        get() {
            val mode = TestDataManagerMode.currentMode
            if (mode != TestDataManagerMode.UPDATE) return mode

            @OptIn(TestInfrastructureInternals::class)
            val expectsFailures = testServices.testConfiguration.failureSuppressors.any { it.expectsFailures() }
            return if (expectsFailures) TestDataManagerMode.CHECK else mode
        }

    /**
     * Remembers [baseTestDataFile] as the base test data file of the test and returns the file the test reads: the expected file.
     */
    internal fun initBaseTestDataFile(baseTestDataFile: File): File {
        this.baseTestDataFile = baseTestDataFile
        return ManagedTestAssertions.findExpectedFile(baseTestDataFile.toPath(), variantChain, extension).toFile()
    }

    /**
     * The extension of the test data, which its variants go before, e.g., `.test.kts` for `foo.ll.test.kts`.
     */
    private val extension: String
        get() = baseTestDataFile.extensionWithDot

    /**
     * The test data files the managed assertions of the test create, update, delete or check, see [isWriteTarget].
     */
    private val writeTargetFiles: MutableSet<Path> = mutableSetOf()

    /**
     * Whether [file] is created, updated, deleted or checked by a managed assertion of the test, so no other code may write it, e.g., the
     * legacy update of a failed check of the file under `kotlin.test.update.test.data`.
     */
    internal fun isWriteTarget(file: File): Boolean = file.toPath().normalizedAbsolute() in writeTargetFiles

    /**
     * Marks [file] as created, updated, deleted or checked by a managed assertion of the test, see [isWriteTarget].
     */
    internal fun addWriteTarget(file: File) {
        // `add`, not `+=`: a `Path` is an `Iterable<Path>` of its name elements
        writeTargetFiles.add(file.toPath().normalizedAbsolute())
    }

    /**
     * The variant of the test data the test writes its inline diagnostics to, e.g., `foo.ll.kt` for the `[ll]` variant chain, whether it
     * exists or not.
     */
    val writeTargetFile: File
        get() = ManagedTestAssertions
            .getWriteTargetFile(baseTestDataFile.toPath(), variantChain, extension)
            .normalizedAbsolute()
            .toFile()

    /**
     * The test data the test would read if [writeTargetFile] didn't exist, e.g., `foo.ll.kt` or `foo.kt` for the `[ll, reversed]` variant
     * chain: the base of the test's own variant. `null` for a golden test.
     */
    val fallbackFile: File?
        get() = ManagedTestAssertions.findFallbackFile(baseTestDataFile.toPath(), variantChain, extension)?.toFile()

    /**
     * Whether the frontend didn't analyze some modules, e.g., the compiler stopped before the analysis. Set by the test runner before the
     * handlers compare their dumps, see [assertEqualsToDump].
     */
    var someModulesWereNotAnalyzed: Boolean = false
        internal set

    /**
     * Whether the test crashed in a way it doesn't expect, or the test infrastructure failed. Set by the test runner before the handlers
     * compare their dumps, see [assertEqualsToDump].
     */
    var testCrashed: Boolean = false
        internal set

    /**
     * The variant of the dump with the [dumpExtension] that [assertEqualsToDump] creates, updates or deletes, whether it exists or not, e.g.,
     * `foo.ll.fir.txt` for `.fir.txt` and the `[ll]` variant chain.
     */
    fun dumpWriteTargetFile(dumpExtension: String): File = ManagedTestAssertions.getWriteTargetFile(
        testDataPath = baseTestDataFile.toPath(),
        variantChain = variantChain,
        extension = dumpExtension,
    ).toFile()

    /**
     * Compares [actual] with the dump of the test with the [dumpExtension], e.g., `.fir.txt`: the variants of the dump follow the variant
     * chain of the test data, so the `[ll, reversed]` test compares with the first existing file of `foo.reversed.fir.txt`, `foo.ll.fir.txt`
     * and `foo.fir.txt`, and writes only `foo.reversed.fir.txt`. The dump extension replaces the last extension of the test data, as for
     * other outputs of managed tests, e.g., `1.1.fir.txt` for `1.1.kt` and `foo.kt.fir.txt` for `foo.kt.can-freeze-ide`, so that `1.1.kt`
     * and `1.2.kt` don't share a dump. Without [actual], the test has nothing to dump, so its variant of the dump must not exist.
     *
     * The dump is compared in [TestDataManagerMode.CHECK] mode, like incomplete inline diagnostics, if the test crashed ([testCrashed]),
     * or if it [dependsOnAnalysis] and [someModulesWereNotAnalyzed]. The output of the compiler doesn't depend on the analysis: it is
     * complete if the compiler stops before the analysis.
     */
    fun assertEqualsToDump(
        dumpExtension: String,
        actual: String?,
        sanitizer: (String) -> String = { it },
        dependsOnAnalysis: Boolean = true,
    ) {
        writeTargetFiles.add(dumpWriteTargetFile(dumpExtension).toPath().normalizedAbsolute())
        val mayBeIncomplete = testCrashed || dependsOnAnalysis && someModulesWereNotAnalyzed
        ManagedTestAssertions.assertEqualsToTestDataFile(
            testDataPath = baseTestDataFile.toPath(),
            actual = actual,
            variantChain = variantChain,
            extension = dumpExtension,
            mode = if (mayBeIncomplete) TestDataManagerMode.CHECK else mode,
            sanitizer = sanitizer,
        )
    }

    /**
     * Whether [assertEqualsToTestDataFile] compared complete inline diagnostics and passed: `false` if they were incomplete or if the
     * comparison failed (e.g., couldn't write the output), `null` before the comparison.
     */
    private var isInlineComparisonSuccessful: Boolean? = null

    /**
     * The edits of the test data made before the comparison of the inline diagnostics. They wait for it, as it would overwrite the edits
     * of [writeTargetFile], and as they come from the same analysis: they are dropped if its diagnostics are incomplete or if it
     * fails.
     */
    private val pendingEdits: MutableList<Pair<File, (String) -> String>> = mutableListOf()

    /**
     * Compares [actual] with the expected file. If [isActualComplete] is `false` (e.g., the analysis crashed, so [actual] misses
     * diagnostics), the comparison is in [TestDataManagerMode.CHECK] mode, so that `updateTestData` doesn't write the incomplete output.
     */
    fun assertEqualsToTestDataFile(actual: String, isActualComplete: Boolean) {
        val testDataPath = baseTestDataFile.toPath()
        addWriteTarget(writeTargetFile)
        // `false` until the comparison passes, so that no edit is applied after a failed one
        isInlineComparisonSuccessful = false
        val edits = pendingEdits.toList()
        pendingEdits.clear()
        ManagedTestAssertions.assertEqualsToTestDataFile(
            testDataPath = testDataPath,
            actual = actual,
            variantChain = variantChain,
            extension = extension,
            mode = if (isActualComplete) mode else TestDataManagerMode.CHECK,
        )

        if (!isActualComplete) return
        isInlineComparisonSuccessful = true
        for ([file, edit] in edits) {
            // The comparison might have deleted the file as redundant, or not created the write target
            if (file.exists()) applyEdit(file, edit)
        }
    }

    /**
     * Deletes [writeTargetFile] in `updateTestData` as redundant, e.g., an `.ll.kt` which differs from the test data it falls back to only
     * in the `LL_FIR_DIVERGENCE` directive: the test data manager can't detect such redundancy itself. The deletion goes through
     * [ManagedTestAssertions], so the run reports it. A variant the test didn't compare is left alone, see [isWriteTarget].
     *
     * Returns `false` if the test data isn't updated, so the caller reports the redundancy instead.
     */
    fun deleteRedundantWriteTargetFile(): Boolean {
        if (!updatesTestData) return false
        if (!isWriteTarget(writeTargetFile)) return true
        ManagedTestAssertions.assertEqualsToTestDataFile(
            testDataPath = baseTestDataFile.toPath(),
            actual = null,
            variantChain = variantChain,
            extension = extension,
            mode = TestDataManagerMode.UPDATE,
        )
        return true
    }

    /**
     * Applies [edit] to [writeTargetFile] in `updateTestData` if the comparison of the inline diagnostics creates it. Such a variant is
     * a copy of the test data the test read, so it misses the edits [TestServices.assertFileUnchangedBy] made to the existing test data
     * before the comparison, e.g., the `GENERATED_FIR_TAGS` footer. Does nothing if [writeTargetFile] exists: the caller edits it with the
     * rest of the existing test data.
     */
    fun updateCreatedWriteTargetFile(edit: (String) -> String) {
        @OptIn(TestInfrastructureInternals::class)
        if (!updatesTestData || isInlineComparisonSuccessful != null || !testServices.testConfiguration.metaInfoHandlerEnabled) return
        val writeTargetFile = writeTargetFile
        if (writeTargetFile.exists()) return
        pendingEdits += writeTargetFile to edit
    }

    /**
     * Applies [edit] to [file] in `updateTestData`, see [TestServices.assertFileUnchangedBy]. An edit made before the comparison of the
     * inline diagnostics waits for it, see [pendingEdits].
     */
    internal fun updateFile(file: File, edit: (String) -> String) {
        @OptIn(TestInfrastructureInternals::class)
        if (isInlineComparisonSuccessful == null && testServices.testConfiguration.metaInfoHandlerEnabled) {
            pendingEdits += file to edit
            return
        }

        applyEdit(file, edit)
    }

    private fun applyEdit(file: File, edit: (String) -> String) {
        addWriteTarget(file)
        ManagedTestAssertions.updateTestDataFile(
            testDataPath = baseTestDataFile.toPath(),
            file = file.toPath(),
            actual = edit(file.readText()),
        )
    }

    private fun Path.normalizedAbsolute(): Path = toAbsolutePath().normalize()
}

val TestServices.managedTestData: ManagedTestData? by TestServices.nullableTestServiceAccessor()

/**
 * The test data file without a variant: the base test data file of a managed test, or the original test data file of another one.
 */
val TestServices.baseTestDataFile: File
    get() = managedTestData?.baseTestDataFile ?: moduleStructure.originalTestDataFiles.first().originalTestDataFile

/**
 * Checks that [edit] doesn't change [file], e.g., that every variant of the test data has a directive. `updateTestData` applies [edit] to
 * the test data of a managed test instead.
 */
fun TestServices.assertFileUnchangedBy(
    file: File,
    message: () -> String = { "Actual data differs from file content" },
    edit: (String) -> String,
) {
    val managedTestData = managedTestData
    if (managedTestData != null && managedTestData.updatesTestData) {
        managedTestData.updateFile(file, edit)
    } else {
        // Only `updateTestData` applies the edit to the test data of a managed test
        managedTestData?.addWriteTarget(file)
        assertions.assertEqualsToFile(file, edit(file.readText()), message = message)
    }
}

/**
 * Makes a test with [ManagedTestData] read the expected file of its test data.
 *
 * It has to run before other configurators which replace the test data with its variants, so that it gets the base test data file.
 */
class ManagedTestDataConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val managedTestData = testServices.managedTestData ?: return testDataFileName
        val testDataFile = File(testDataFileName)
        val expectedFile = managedTestData.initBaseTestDataFile(testDataFile)
        return if (expectedFile == testDataFile) testDataFileName else expectedFile.path
    }
}
