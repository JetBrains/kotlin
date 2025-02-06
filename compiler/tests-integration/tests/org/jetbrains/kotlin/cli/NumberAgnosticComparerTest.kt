/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import junit.framework.TestCase
import kotlin.test.assertNotEquals

class NumberAgnosticComparerTest : TestCase() {
    companion object {
        private val INT = NumberAgnosticComparer.generatePlaceholder(NumberAgnosticComparer.INT_MARKER)
        private val REAL = NumberAgnosticComparer.generatePlaceholder(NumberAgnosticComparer.REAL_MARKER)
    }

    fun testPlaceholders() {
        assertExpectEqualsActual("$INT   $INT   $REAL   $REAL", "0   123   45.67   .89")
    }

    fun testMismatchedTypes() {
        assertExpectNotEqualsActual("$INT $REAL", "0.25 54", "$REAL $INT")
    }

    private val standardPaddingCount = 8
    private val numberThatFitsPadding = 1234
    private val numberThatDoesntFitPadding = 12345678901
    private val paddingSpaces = buildPaddingString(standardPaddingCount)
    private val textBefore = "text_before:"
    private val textAfter = ":text_after"
    private val incorrectPaddingInExpect = 4

    fun testRightAlignment() {
        val intRightPlaceholder =
            NumberAgnosticComparer.generatePlaceholder(NumberAgnosticComparer.INT_MARKER, NumberAgnosticComparer.Alignment.Right)
        val expectedText = textBefore + paddingSpaces + intRightPlaceholder + textAfter
        val rightPaddingFormat = "%${standardPaddingCount}s"
        val actualNormalText = textBefore + String.format(rightPaddingFormat, numberThatFitsPadding) + textAfter

        // typical case: a value fits padding count
        assertExpectEqualsActual(expectedText, actualNormalText)

        // expect template is incorrect -> it requires increasing the number of spaces left to $INT$ placeholder (to 8)
        val incorrectExpectedText = textBefore + buildPaddingString(incorrectPaddingInExpect) + intRightPlaceholder + textAfter
        assertExpectNotEqualsActual(
            incorrectExpectedText,
            actualNormalText,
            textBefore + paddingSpaces + intRightPlaceholder + textAfter,
        )

        // a value exceeds padding count -> it requires increasing the number of padding spaces in formatting code (11 instead of 8)
        val longNumberToString = String.format(rightPaddingFormat, numberThatDoesntFitPadding)
        assertExpectNotEqualsActual(
            expectedText,
            textBefore + longNumberToString + textAfter,
            textBefore + buildPaddingString(longNumberToString.length) + intRightPlaceholder + textAfter,
        )
    }

    fun testLeftAlignment() {
        val intLeftPlaceholder =
            NumberAgnosticComparer.generatePlaceholder(NumberAgnosticComparer.INT_MARKER, NumberAgnosticComparer.Alignment.Left)
        val expectedText = textBefore + intLeftPlaceholder + paddingSpaces + textAfter
        val leftPaddingFormat = "%-${standardPaddingCount}s"
        val actualNormalText = textBefore + String.format(leftPaddingFormat, numberThatFitsPadding) + textAfter

        // typical case: a value fits padding count
        assertExpectEqualsActual(expectedText, actualNormalText)

        // The expected template is incorrect -> it requires increasing the number of spaces right to $INT$ placeholder (to 8)
        val incorrectExpectedText = textBefore + intLeftPlaceholder + buildPaddingString(incorrectPaddingInExpect) + textAfter
        assertExpectNotEqualsActual(
            incorrectExpectedText,
            actualNormalText,
            textBefore + intLeftPlaceholder + paddingSpaces + textAfter,
        )

        // a value exceeds padding count -> it requires increasing the number of padding spaces in formatting code (11 instead of 8)
        val longNumberToString = String.format(leftPaddingFormat, numberThatDoesntFitPadding)
        assertExpectNotEqualsActual(
            expectedText,
            textBefore + longNumberToString + textAfter,
            textBefore + intLeftPlaceholder + buildPaddingString(longNumberToString.length) + textAfter,
        )
    }

    private fun buildPaddingString(count: Int): String = buildString { repeat(count) { append(' ') } }

    fun testSpecifiedNumberLiterals() {
        assertExpectEqualsActual("42 0.53", "42 0.53")
    }

    fun testMismatchedNumberLiterals() {
        assertExpectNotEqualsActual("5 32.0", "43 0.85", "43 0.85")
    }

    fun testEmptyExpect() {
        // Generate placeholders on an empty expected file
        assertExpectNotEqualsActual("", "1234 56.68", "$INT $REAL")
    }

    fun testMismatchedExpect() {
        assertExpectNotEqualsActual(
            """
            first_line
            $INT mismatched_number
            last_line
            """.trimIndent(),

            """
            100 first_line
            256 mismatched_number
            last_line
            """.trimIndent(),

            """
            $INT first_line
            $INT mismatched_number
            last_line
            """.trimIndent()
        )
    }

    private fun assertExpectEqualsActual(expected: String, actual: String) {
        assertEquals(expected, getSanitizedActual(expected, actual))
    }

    private fun assertExpectNotEqualsActual(expected: String, actual: String, resultActual: String) {
        val sanitizedActual = getSanitizedActual(expected, actual)
        assertNotEquals(expected, sanitizedActual)
        assertEquals(resultActual, sanitizedActual)
    }

    private fun getSanitizedActual(expectedText: String, actualText: String): String {
        val comparer = NumberAgnosticComparer(actualText)
        return comparer.generatedSanitizedActualTextBasedOnExpectPlaceholders(expectedText)
    }
}