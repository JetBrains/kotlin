/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VariantChainComparatorTest {
    private fun assertSorted(variantChains: List<List<String>>, expected: String) {
        val sorted = variantChains.sortedWith(VariantChainComparator)
        val actual = sorted.joinToString("\n") { it.toString() }
        assertEquals(expected.trimIndent(), actual)
    }

    @Test
    fun `empty list comes first`() {
        assertSorted(
            variantChains = [
                ["descriptors"],
                [],
                ["standalone", "fir"],
            ],
            expected = """
                []
                [descriptors]
                [standalone, fir]
            """
        )
    }

    @Test
    fun `shorter chains before longer`() {
        assertSorted(
            variantChains = [
                ["a", "b", "c"],
                ["x"],
                ["m", "n"],
            ],
            expected = """
                [x]
                [m, n]
                [a, b, c]
            """
        )
    }

    @Test
    fun `alphabetical ordering for same depth`() {
        assertSorted(
            variantChains = [
                ["zebra"],
                ["alpha"],
                ["beta"],
            ],
            expected = """
                [alpha]
                [beta]
                [zebra]
            """
        )
    }

    @Test
    fun `alphabetical ordering for multi-element chains`() {
        assertSorted(
            variantChains = [
                ["standalone", "fir"],
                ["standalone", "descriptors"],
                ["analysis", "api"],
            ],
            expected = """
                [analysis, api]
                [standalone, descriptors]
                [standalone, fir]
            """
        )
    }

    @Test
    fun `combined ordering - empty first, then by depth, then alphabetically`() {
        assertSorted(
            variantChains = [
                ["standalone", "fir"],
                ["descriptors"],
                [],
                ["alpha"],
                ["a", "b", "c"],
            ],
            expected = """
                []
                [alpha]
                [descriptors]
                [standalone, fir]
                [a, b, c]
            """
        )
    }

    @Test
    fun `identical chains are equal`() {
        val result = VariantChainComparator.compare(
            ["a", "b"],
            ["a", "b"],
        )

        assertEquals(0, result)
    }

    @Test
    fun `two empty chains are equal`() {
        val result = VariantChainComparator.compare([], [])

        assertEquals(0, result)
    }
}
