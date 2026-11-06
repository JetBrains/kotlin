/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import java.nio.file.Path

/**
 * Represents a discovered test with its variant chain information.
 */
internal data class DiscoveredTest(
    val uniqueId: String,
    val displayName: String,
    val variantChain: TestVariantChain,
    val testDataPath: String? = null,
)

/**
 * Represents a group of tests with the same variant chain depth.
 * See README.md for grouping rules.
 */
internal data class TestGroup(
    val variantDepth: Int,
    val tests: List<DiscoveredTest>,
) {
    /** Unique variant chains in this group, sorted by [VariantChainComparator]. */
    val uniqueVariantChains: List<List<String>> by lazy {
        tests.mapTo(HashSet()) { it.variantChain }.sortedWith(VariantChainComparator)
    }
}

/**
 * Result of grouping tests by variant chain depth.
 */
internal data class GroupingResult(
    val groups: List<TestGroup>,
    val conflicts: List<VariantChainConflict>,
)

/**
 * Describes a conflict between two variant chains.
 * See README.md for conflict rules.
 */
internal data class VariantChainConflict(
    val chainA: List<String>,
    val chainB: List<String>,
    val conflictingVariant: String,
    val reason: String,
) {
    fun format(): String = reason
}

/**
 * A failed test whose failure is not a [TestDataProblem].
 *
 * @property testName the test as `Class.method`
 * @property testDataPath the `@TestMetadata` path of the test, if any
 * @property exceptionClass the fully qualified class of the exception
 * @property stackTrace the top frames of the exception, down to the first frame of the test class
 */
internal data class TestError(
    val testName: String,
    val testDataPath: String?,
    val variantChain: TestVariantChain,
    val exceptionClass: String,
    val message: String?,
    val stackTrace: List<String>,
) {
    /** One line for the console summary: the simple class name and the start of the message. */
    val exception: String
        get() {
            val name = exceptionClass.substringAfterLast('.')
            val shortMessage = message?.lineSequence()?.first()?.let { if (it.length > 100) it.take(100) + "..." else it }
            return if (shortMessage.isNullOrEmpty()) name else "$name: $shortMessage"
        }
}

/**
 * A test data file that doesn't match the output of [testName], from a [TestDataAssertionError].
 *
 * @property file the file `updateTestData` would create, update or delete
 * @property comparedFile the existing file the output was compared with, see [TestDataAssertionError.comparedFile]
 * @property actual the actual output, for the kinds that compare it with a file
 */
internal data class TestDataProblem(
    val testName: String,
    val testDataPath: String?,
    val variantChain: TestVariantChain,
    val kind: TestDataAssertionError.Kind,
    val file: Path,
    val comparedFile: Path?,
    val actual: String?,
)

/**
 * A test data file changed by [ManagedTestAssertions] for the test of [testDataPath].
 */
internal data class FileAction(
    val testDataPath: Path,
    val file: Path,
    val kind: Kind,
) {
    enum class Kind {
        CREATED,
        UPDATED,
        DELETED,
        ;

        val existedBefore: Boolean get() = this != CREATED
        val existsAfter: Boolean get() = this != DELETED

        companion object {
            /** The net effect on a file that [existedBefore] and [existsAfter], or `null` if there is none. */
            fun of(existedBefore: Boolean, existsAfter: Boolean): Kind? = when {
                !existedBefore && existsAfter -> CREATED
                existedBefore && !existsAfter -> DELETED
                existedBefore && existsAfter -> UPDATED
                else -> null
            }
        }
    }
}

/**
 * The actions of this list followed by the [later] ones, one per file with the net effect of both.
 */
internal fun List<FileAction>.mergedWith(later: List<FileAction>): List<FileAction> {
    val laterByFile = later.associateBy { it.file }
    val merged = mapNotNull { earlier ->
        val next = laterByFile[earlier.file] ?: return@mapNotNull earlier
        val kind = FileAction.Kind.of(earlier.kind.existedBefore, next.kind.existsAfter) ?: return@mapNotNull null
        earlier.copy(kind = kind)
    }

    val earlierFiles = mapTo(HashSet()) { it.file }
    return merged + later.filter { it.file !in earlierFiles }
}

/**
 * A test that failed in a pass of the convergence loop and passed when it was re-run: a flaky test, or one that needed
 * files other tests of its group wrote in that pass. It counts as passed and doesn't fail the run.
 *
 * @property passedInPass the pass it passed in, from 2
 * @property problems the test data problems of its last failing pass
 * @property errors the other failures of its last failing pass
 */
internal data class RecoveredTest(
    val testName: String,
    val passedInPass: Int,
    val problems: List<TestDataProblem>,
    val errors: List<TestError>,
)
