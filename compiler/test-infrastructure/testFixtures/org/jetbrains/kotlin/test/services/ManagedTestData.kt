/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerMode
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.impl.testConfiguration
import java.io.File

/**
 * Lets `checkTestData` and `updateTestData` manage the test data with inline diagnostics of a test implementing [ManagedTest].
 *
 * A variant of the test data is the base test data file with the variant before its extension, e.g., `foo.ll.kt` for the `ll`
 * variant of `foo.kt`. Such a file is both the input of the test and its expected output: [ManagedTestDataConfigurator] makes the
 * test read the expected file of its [variantChain], and [assertEqualsToTestDataFile] compares the actual output with the same file
 * via [ManagedTestAssertions].
 */
class ManagedTestData(private val testServices: TestServices, private val variantChain: List<String>) : TestService {
    /**
     * The test data file without a variant, which [ManagedTestDataConfigurator] replaces with the expected file.
     */
    private lateinit var baseTestDataFile: File

    /**
     * The mode of the comparison. A muted test is compared in [TestDataManagerMode.CHECK] mode even by `updateTestData`, as its output
     * is expected to be wrong.
     */
    private val mode: TestDataManagerMode
        get() {
            val mode = TestDataManagerMode.currentMode
            if (mode != TestDataManagerMode.UPDATE) return mode

            @OptIn(TestInfrastructureInternals::class)
            val isMuted = testServices.testConfiguration.failureSuppressors.any { it.testIsMuted() }
            return if (isMuted) TestDataManagerMode.CHECK else mode
        }

    /**
     * Remembers [baseTestDataFile] as the base test data file of the test and returns the file the test reads: the expected file.
     */
    internal fun initBaseTestDataFile(baseTestDataFile: File): File {
        this.baseTestDataFile = baseTestDataFile
        return ManagedTestAssertions.findExpectedFile(baseTestDataFile.toPath(), variantChain, baseTestDataFile.extension).toFile()
    }

    /**
     * Compares [actual] with the expected file. If [isActualComplete] is `false` (e.g., the analysis crashed, so [actual] misses
     * diagnostics), the comparison is in [TestDataManagerMode.CHECK] mode, so that `updateTestData` doesn't write the incomplete output.
     */
    fun assertEqualsToTestDataFile(actual: String, isActualComplete: Boolean) {
        ManagedTestAssertions.assertEqualsToTestDataFile(
            testDataPath = baseTestDataFile.toPath(),
            actual = actual,
            variantChain = variantChain,
            extension = baseTestDataFile.extension,
            mode = if (isActualComplete) mode else TestDataManagerMode.CHECK,
        )
    }
}

val TestServices.managedTestData: ManagedTestData? by TestServices.nullableTestServiceAccessor()

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
