/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest

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

        val boundedHeadOnly = text.toBoundedDiagnostic(16)

        assertEquals(16, boundedHeadOnly.length)
        assertEquals("... [truncated; ", boundedHeadOnly)
    }

    @Test
    fun `given text shorter than the bound but longer than the suffix then it is returned unchanged`() {
        val text = "x".repeat(200)
        val maxLength = 300
        assertTrue(text.suffix().length < text.length)
        assertTrue(text.length < maxLength)

        assertTrue(text === text.toBoundedDiagnostic(maxLength))
        assertTrue(text === text.toBoundedDiagnosticKeepingTail(maxLength))
    }

    @Test
    fun `given text shorter than both the bound and the suffix then it is returned unchanged`() {
        val text = "short"
        val maxLength = 1_000
        assertTrue(text.length < text.suffix().length)
        assertTrue(text.suffix().length < maxLength)

        assertTrue(text === text.toBoundedDiagnostic(maxLength))
        assertTrue(text === text.toBoundedDiagnosticKeepingTail(maxLength))
    }

    @Test
    fun `given text within a bound that is itself smaller than the suffix then it is returned unchanged`() {
        val text = "short"
        val maxLength = 8
        assertTrue(text.length < maxLength)
        assertTrue(maxLength < text.suffix().length)

        assertTrue(text === text.toBoundedDiagnostic(maxLength))
        assertTrue(text === text.toBoundedDiagnosticKeepingTail(maxLength))
    }

    @Test
    fun `given text exactly at the bound then it is returned unchanged`() {
        val text = "x".repeat(50)

        assertTrue(text === text.toBoundedDiagnostic(50))
        assertTrue(text === text.toBoundedDiagnosticKeepingTail(50))
        assertTrue("" === "".toBoundedDiagnostic(0))
        assertTrue("" === "".toBoundedDiagnosticKeepingTail(0))
    }

    @Test
    fun `given a zero bound for a non-empty text then nothing is returned`() {
        val text = "x".repeat(10)

        assertEquals("", text.toBoundedDiagnostic(0))
        assertEquals("", text.toBoundedDiagnosticKeepingTail(0))
    }

    @Test
    fun `given a bound equal to the suffix then the suffix alone is returned`() {
        val text = "x".repeat(1_000)
        val suffix = text.suffix()
        val marker = text.marker()

        assertEquals(suffix, text.toBoundedDiagnostic(suffix.length))
        assertEquals(marker, text.toBoundedDiagnosticKeepingTail(marker.length))
    }

    @Test
    fun `given text one character over the bound then it is cut down to the bound`() {
        val text = ('a'..'z').joinToString("").repeat(40)
        val maxLength = text.length - 1
        val suffix = text.suffix()
        val marker = text.marker()

        assertEquals(text.take(maxLength - suffix.length) + suffix, text.toBoundedDiagnostic(maxLength))

        val retained = maxLength - marker.length
        val headLength = retained / 2
        val tailLength = retained - headLength
        assertEquals(text.take(headLength) + marker + text.takeLast(tailLength), text.toBoundedDiagnosticKeepingTail(maxLength))
    }

    @Test
    fun `given any bound below the text length then the result fills the bound exactly`() {
        val text = ('a'..'z').joinToString("").repeat(20)
        val suffix = text.suffix()
        val marker = text.marker()

        for (maxLength in 0 until text.length) {
            val bounded = text.toBoundedDiagnostic(maxLength)
            assertEquals(maxLength, bounded.length, "toBoundedDiagnostic($maxLength)")
            val expected = if (maxLength < suffix.length) suffix.take(maxLength) else text.take(maxLength - suffix.length) + suffix
            assertEquals(expected, bounded, "toBoundedDiagnostic($maxLength)")

            val boundedKeepingTail = text.toBoundedDiagnosticKeepingTail(maxLength)
            assertEquals(maxLength, boundedKeepingTail.length, "toBoundedDiagnosticKeepingTail($maxLength)")
            if (maxLength <= marker.length) {
                assertEquals(marker.take(maxLength), boundedKeepingTail, "toBoundedDiagnosticKeepingTail($maxLength)")
            } else {
                assertTrue(marker in boundedKeepingTail, "toBoundedDiagnosticKeepingTail($maxLength)")
                assertTrue(text.startsWith(boundedKeepingTail.substringBefore(marker)), "toBoundedDiagnosticKeepingTail($maxLength)")
                assertTrue(text.endsWith(boundedKeepingTail.substringAfter(marker)), "toBoundedDiagnosticKeepingTail($maxLength)")
            }
        }
    }

    @Test
    fun `given a surrogate pair on the digest chunk boundary then the digest matches the whole text`() {
        val text = "x".repeat(4_095) + "\uD83D\uDE00" + "y".repeat(10)
        val expected = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).toHexString()

        assertEquals(expected, text.sha256Hex())
    }

    private fun String.suffix(): String = "... [truncated; original length=$length chars; SHA-256=${sha256Hex()}]"

    private fun String.marker(): String =
        "\n... [truncated; original length=$length chars; SHA-256=${sha256Hex()}; middle omitted] ...\n"
}
