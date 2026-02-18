/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

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
