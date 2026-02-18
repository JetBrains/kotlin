/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner.groupByVariantDepth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for [groupByVariantDepth] and [validateConflicts] functions.
 *
 * These tests verify:
 * 1. Correct grouping of tests by variant chain depth (not exact chain)
 * 2. Conflict detection when tests interfere with each other
 */
class TestDataManagerGroupingTest {
    private fun assertGrouping(tests: List<DiscoveredTest>, expected: String) {
        val result = groupByVariantDepth(tests)
        val actual = result.groups.joinToString("\n") { group ->
            "depth=${group.variantDepth}: ${group.uniqueVariantChains.joinToString(", ")}"
        }
        assertEquals(expected.trimIndent(), actual)
    }

    private fun assertConflicts(tests: List<DiscoveredTest>, expected: String) {
        val conflicts = validateConflicts(tests)
        val actual = conflicts.joinToString("\n") {
            "${it.chainA} vs ${it.chainB}: '${it.conflictingVariant}'"
        }
        assertEquals(expected.trimIndent(), actual)
    }

    // === Grouping tests ===

    @Test
    fun `tests grouped by variant depth`() {
        assertGrouping(
            tests = listOf(
                DiscoveredTest("1", "golden", emptyList()),
                DiscoveredTest("2", "js", listOf("js")),
                DiscoveredTest("3", "wasm", listOf("wasm")),
                DiscoveredTest("4", "knm-js", listOf("knm", "js")),
            ),
            expected = """
                depth=0: []
                depth=1: [js], [wasm]
                depth=2: [knm, js]
            """
        )
    }

    @Test
    fun `all single-variant tests in one group`() {
        assertGrouping(
            tests = listOf(
                DiscoveredTest("1", "js", listOf("js")),
                DiscoveredTest("2", "wasm", listOf("wasm")),
                DiscoveredTest("3", "knm", listOf("knm")),
                DiscoveredTest("4", "native", listOf("native")),
            ),
            expected = """
                depth=1: [js], [knm], [native], [wasm]
            """
        )
    }

    @Test
    fun `multiple tests with same variant in same group`() {
        assertGrouping(
            tests = listOf(
                DiscoveredTest("1", "js-test-1", listOf("js")),
                DiscoveredTest("2", "js-test-2", listOf("js")),
                DiscoveredTest("3", "wasm-test", listOf("wasm")),
            ),
            expected = """
                depth=1: [js], [wasm]
            """
        )
    }

    @Test
    fun `empty tests list produces empty groups`() {
        val result = groupByVariantDepth(emptyList())
        assertEquals(emptyList<TestGroup>(), result.groups)
        assertEquals(emptyList<VariantChainConflict>(), result.conflicts)
    }

    @Test
    fun `golden only produces single group`() {
        assertGrouping(
            tests = listOf(
                DiscoveredTest("1", "golden-1", emptyList()),
                DiscoveredTest("2", "golden-2", emptyList()),
            ),
            expected = """
                depth=0: []
            """
        )
    }

    // === Conflict detection tests ===

    @Test
    fun `conflict detected - same last variant`() {
        assertConflicts(
            tests = listOf(
                DiscoveredTest("1", "a-b-c", listOf("a", "b", "c")),
                DiscoveredTest("2", "x-y-c", listOf("x", "y", "c")),
            ),
            expected = """
                [a, b, c] vs [x, y, c]: 'c'
            """
        )
    }

    @Test
    fun `conflict detected - last variant in other chain`() {
        assertConflicts(
            tests = listOf(
                DiscoveredTest("1", "a-b-c", listOf("a", "b", "c")),
                DiscoveredTest("2", "a-c-b", listOf("a", "c", "b")),
            ),
            expected = """
                [a, b, c] vs [a, c, b]: 'c'
                [a, b, c] vs [a, c, b]: 'b'
            """
        )
    }

    @Test
    fun `no conflict - different last variants, no overlap`() {
        assertConflicts(
            tests = listOf(
                DiscoveredTest("1", "js", listOf("js")),
                DiscoveredTest("2", "wasm", listOf("wasm")),
            ),
            expected = ""
        )
    }

    @Test
    fun `no conflict - different platforms with same base`() {
        assertConflicts(
            tests = listOf(
                DiscoveredTest("1", "lib-js", listOf("lib", "js")),
                DiscoveredTest("2", "lib-wasm", listOf("lib", "wasm")),
                DiscoveredTest("3", "lib-native", listOf("lib", "native")),
            ),
            expected = ""
        )
    }

    @Test
    fun `no conflict - empty variant chains`() {
        assertConflicts(
            tests = listOf(
                DiscoveredTest("1", "golden-1", emptyList()),
                DiscoveredTest("2", "golden-2", emptyList()),
            ),
            expected = ""
        )
    }

    @Test
    fun `no conflict - single test`() {
        assertConflicts(
            tests = listOf(
                DiscoveredTest("1", "single", listOf("a", "b", "c")),
            ),
            expected = ""
        )
    }

    @Test
    fun `conflict in one group does not affect other groups`() {
        val tests = listOf(
            // Group 0 - no conflict possible
            DiscoveredTest("1", "golden", emptyList()),
            // Group 1 - no conflict
            DiscoveredTest("2", "js", listOf("js")),
            DiscoveredTest("3", "wasm", listOf("wasm")),
            // Group 2 - has conflict
            DiscoveredTest("4", "a-b", listOf("a", "b")),
            DiscoveredTest("5", "b-a", listOf("b", "a")),
        )

        val result = groupByVariantDepth(tests)

        // Should have 3 groups
        assertEquals(3, result.groups.size)

        // Should detect the conflict in group 2
        assertEquals(2, result.conflicts.size)
        assertEquals("b", result.conflicts[0].conflictingVariant)
        assertEquals("a", result.conflicts[1].conflictingVariant)
    }

    // === Integration: groupByVariantDepth includes conflict validation ===

    @Test
    fun `groupByVariantDepth returns conflicts`() {
        val tests = listOf(
            DiscoveredTest("1", "a-b-c", listOf("a", "b", "c")),
            DiscoveredTest("2", "x-y-c", listOf("x", "y", "c")),
        )

        val result = groupByVariantDepth(tests)

        assertEquals(1, result.groups.size)
        assertEquals(1, result.conflicts.size)
        assertEquals("c", result.conflicts[0].conflictingVariant)
    }
}
