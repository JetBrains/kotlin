/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.report

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.analysis.test.data.manager.FileActionKind
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerMode
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataProblemKind

/** The JSON of the module reports and of the summary of the wrapper: indented by two spaces, with `null` values written. */
@OptIn(ExperimentalSerializationApi::class)
val TEST_DATA_MANAGER_JSON: Json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    ignoreUnknownKeys = true
}

enum class RunStatus {
    /** All tests passed. */
    PASSED,

    /** Some tests have problems or test errors. */
    FAILED,

    /** The run couldn't start or finish, see its run errors. */
    ERROR,
}

@Serializable
data class TestCounts(val discovered: Int, val passed: Int, val failed: Int, val skipped: Int) {
    operator fun plus(other: TestCounts): TestCounts =
        TestCounts(discovered + other.discovered, passed + other.passed, failed + other.failed, skipped + other.skipped)

    companion object {
        val ZERO: TestCounts = TestCounts(0, 0, 0, 0)
    }
}

/**
 * What one run of the test data manager did in one module, `<runId>.json` in the report directory of the module.
 *
 * Paths inside the repository are relative to its root, except [Problem.actualFile], which is absolute.
 */
@Serializable
data class ModuleReport(
    val schemaVersion: Int,
    val runId: String,
    val project: String?,
    val mode: TestDataManagerMode?,
    val status: RunStatus,
    val options: Options,
    /** Errors that stopped the run itself: invalid options, variant chain conflicts, crashes of the runner. */
    val runErrors: List<String>,
    val tests: TestCounts,
    val problems: List<Problem>,
    val testErrors: List<TestError>,
    val fileActions: List<FileAction>,
    /** Tests that passed only on a rerun; they don't fail the run. */
    val recoveredTests: List<RecoveredTest>,
) {
    @Serializable
    data class Options(
        @SerialName("testDataPath")
        val testDataPaths: List<String>?,
        val testClassPattern: String?,
        val goldenOnly: Boolean,
        val incremental: Boolean,
    )

    /** A test data file that doesn't match what its [test] produced; [actualFile] holds the actual output, if any. */
    @Serializable
    data class Problem(
        val kind: TestDataProblemKind,
        val file: String,
        val comparedFile: String?,
        val actualFile: String?,
        val test: String,
        val testDataPath: String?,
        val variantChain: List<String>,
    )

    @Serializable
    data class TestError(
        val test: String,
        val testDataPath: String?,
        val variantChain: List<String>,
        val exception: String,
        val message: String?,
        val stackTrace: List<String>,
    )

    /** A test data file the run created, updated or deleted. */
    @Serializable
    data class FileAction(val kind: FileActionKind, val file: String, val testDataPath: String)

    @Serializable
    data class RecoveredTest(val test: String, val passedInPass: Int, val failures: List<String>)

    companion object {
        const val SCHEMA_VERSION: Int = 1

        /** The runner prints the path of the report it wrote in a line of its own, after this prefix. */
        const val PATH_PREFIX: String = "Test data manager report: "
    }
}
