/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestDiagnosticUtilsTest {
    @Test
    fun `given text within the bound then it is returned unchanged`() {
        val text = "head\n" + "x".repeat(100) + "\ntail"

        assertTrue(text === text.toBoundedDiagnostic(text.length))
        assertTrue(text === text.toBoundedDiagnosticKeepingTail(text.length))
    }

    @Test
    fun `given text over the bound then keeping the tail retains both ends within the bound`() {
        val head = "head-of-output:"
        val tail = ":RuntimeError: unreachable"
        val text = head + "x".repeat(100_000) + tail
        val maxLength = 4 * 1024

        val bounded = text.toBoundedDiagnosticKeepingTail(maxLength)

        assertTrue(bounded.length <= maxLength, bounded.length.toString())
        assertTrue(bounded.startsWith(head), bounded.take(64))
        assertTrue(bounded.endsWith(tail), bounded.takeLast(64))
        assertTrue("truncated; original length=${text.length} chars; SHA-256=${text.sha256Hex()}" in bounded, bounded)
    }

    @Test
    fun `given a bound smaller than the marker then only the marker head is returned`() {
        val text = "x".repeat(1_000)

        val bounded = text.toBoundedDiagnosticKeepingTail(16)

        assertEquals(16, bounded.length)
        assertTrue(bounded.startsWith("\n... [truncated"), bounded)
    }
}
