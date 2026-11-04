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
 * Represents a test error with formatted test name and exception.
 */
internal data class TestError(
    val testName: String,
    val exception: String,
)

/**
 * A test data file that doesn't match the output of [testName], from a [TestDataAssertionError].
 */
internal data class TestDataProblem(
    val testName: String,
    val kind: TestDataAssertionError.Kind,
    val file: Path,
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
