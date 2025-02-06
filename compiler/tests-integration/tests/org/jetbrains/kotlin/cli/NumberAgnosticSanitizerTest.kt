/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse

class NumberAgnosticSanitizerTest : TestCase() {
    companion object {
        private val INT = NumberAgnosticSanitizer.generatePlaceholder(NumberAgnosticSanitizer.INT_MARKER)
        private val REAL = NumberAgnosticSanitizer.generatePlaceholder(NumberAgnosticSanitizer.REAL_MARKER)
    }

    fun testPlaceholders() {
        assertExpectEqualsActual("$INT   $INT   $REAL   $REAL", "0   123   45.67   .89")
    }

    fun testMismatchedTypes() {
        assertExpectNotEqualsActual("$INT $REAL", "0.25 54", "$REAL $INT")
    }

    fun testNumberLiterals() {
        assertExpectEqualsActual("42 0.53", "42 0.53")
    }

    fun testMismatchedNumberLiterals() {
        assertExpectNotEqualsActual("5 32.0", "43 0.85", "43 0.85")
    }

    fun testEmptyExpect() {
        // Generate placeholders on nonexisting/empty expected file
        assertExpectNotEqualsActual("", "1234 56.68", "$INT $REAL")
    }

    fun testMismatchedPlaceholdersAndNumbers() {
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
}

class NumberAgnosticComparerAlignmentTest : TestCase() {
    private val standardPaddingCount = 8
    private val numberThatFitsPadding = 123L
    private val numberThatDoesntFitPadding = 12345678901L
    private val textBefore = "text_before:"
    private val textAfter = ":text_after"
    private val incorrectSmallPaddingInExpect = 4
    private val incorrectBigPaddingInExpect = 12

    // Typical case: a value fits padding count
    fun testRightAlignmentTypical() {
        checkAlignmentTypical(NumberAgnosticSanitizer.Alignment.Right)
    }

    // Expect template is incorrect -> it requires increasing/decreasing the number of spaces left to $INT$ placeholder
    fun testRightAlignmentWhenPaddingSpacesInExpectAreIncorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectSmallPaddingInExpect, NumberAgnosticSanitizer.Alignment.Right)
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectBigPaddingInExpect, NumberAgnosticSanitizer.Alignment.Right)
    }

    // A value exceeds padding count -> it requires increasing the number of padding spaces in code that prints this string
    fun testRightAlignmentWhenPrintedStringExceedsPaddingSpaces() {
        checkAlignmentWhenPrintedStringExceedPaddingSpaces(NumberAgnosticSanitizer.Alignment.Right)
    }

    // Incorrect marker in expect template -> it requires fixing the marker
    fun testRightAlignmentWhenExpectAlignmentMarkerIsIncorrect() {
        checkAlignmentWhenIncorrectMarkerInExpect(NumberAgnosticSanitizer.Alignment.Right, NumberAgnosticSanitizer.Alignment.Left)
        checkAlignmentWhenIncorrectMarkerInExpect(NumberAgnosticSanitizer.Alignment.Right, NumberAgnosticSanitizer.Alignment.None)
    }

    // Typical case: a value fits padding count
    fun testLeftAlignmentTypical() {
        checkAlignmentTypical(NumberAgnosticSanitizer.Alignment.Left)
    }

    // Expect template is incorrect -> it requires increasing/decreasing the number of spaces right to $INT$ placeholder
    fun testLeftAlignmentWhenPaddingSpacesInExpectAreIncorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectSmallPaddingInExpect, NumberAgnosticSanitizer.Alignment.Left)
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectBigPaddingInExpect, NumberAgnosticSanitizer.Alignment.Left)
    }

    // A value exceeds padding count -> it requires increasing the number of padding spaces in code that prints this string
    fun testLeftAlignmentWhenPrintedStringExceedsPaddingSpaces() {
        checkAlignmentWhenPrintedStringExceedPaddingSpaces(NumberAgnosticSanitizer.Alignment.Left)
    }

    // Incorrect marker in expect template -> it requires fixing the marker
    fun testLeftAlignmentWhenExpectAlignmentMarkerIsIncorrect() {
        checkAlignmentWhenIncorrectMarkerInExpect(NumberAgnosticSanitizer.Alignment.Left, NumberAgnosticSanitizer.Alignment.Right)
        checkAlignmentWhenIncorrectMarkerInExpect(NumberAgnosticSanitizer.Alignment.Left, NumberAgnosticSanitizer.Alignment.None)
    }

    private fun checkAlignmentTypical(alignment: NumberAgnosticSanitizer.Alignment) {
        val expectedText =
            textBefore + generateIntPlaceholderWithPaddingSpaces(standardPaddingCount, alignment) + textAfter
        val actualNormalText =
            textBefore + generateStringFormat(numberThatFitsPadding, alignment) + textAfter
        assertExpectEqualsActual(expectedText, actualNormalText)
    }

    private fun checkAlignmentWhenIncorrectMarkerInExpect(
        alignment: NumberAgnosticSanitizer.Alignment,
        incorrectAlignment: NumberAgnosticSanitizer.Alignment
    ) {
        assert(alignment != incorrectAlignment)
        val sanitizedPlaceholderLeftPaddingLength: Int
        val sanitizedPlaceholderRightPaddingLength: Int
        val numberToString = numberThatFitsPadding.toString()
        if (alignment == NumberAgnosticSanitizer.Alignment.Right) {
            sanitizedPlaceholderLeftPaddingLength = standardPaddingCount - numberToString.length
            sanitizedPlaceholderRightPaddingLength =
                if (incorrectAlignment == NumberAgnosticSanitizer.Alignment.Left) numberToString.length else 0
        } else {
            sanitizedPlaceholderLeftPaddingLength =
                if (incorrectAlignment == NumberAgnosticSanitizer.Alignment.Right) numberToString.length else 0
            sanitizedPlaceholderRightPaddingLength = standardPaddingCount - numberToString.length
        }
        val expectedText = textBefore +
                generateIntPlaceholderWithPaddingSpaces(standardPaddingCount, incorrectAlignment) +
                textAfter
        val actualNormalText = textBefore +
                generateStringFormat(numberThatFitsPadding, alignment) +
                textAfter
        val sanitizedActualText = textBefore +
                buildPaddingString(sanitizedPlaceholderLeftPaddingLength) +
                NumberAgnosticSanitizer.generatePlaceholder(NumberAgnosticSanitizer.INT_MARKER, incorrectAlignment) +
                buildPaddingString(sanitizedPlaceholderRightPaddingLength) +
                textAfter
        assertExpectNotEqualsActual(expectedText, actualNormalText, sanitizedActualText)
    }

    private fun checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectPaddingCountInExpect: Int, alignment: NumberAgnosticSanitizer.Alignment) {
        val incorrectExpectedText = textBefore +
                generateIntPlaceholderWithPaddingSpaces(incorrectPaddingCountInExpect, alignment) +
                textAfter
        val actualText = textBefore +
                generateStringFormat(numberThatFitsPadding, alignment) +
                textAfter
        val sanitizedActualText = textBefore +
                generateIntPlaceholderWithPaddingSpaces(standardPaddingCount, alignment) +
                textAfter
        assertExpectNotEqualsActual(incorrectExpectedText, actualText, sanitizedActualText)
    }

    private fun checkAlignmentWhenPrintedStringExceedPaddingSpaces(alignment: NumberAgnosticSanitizer.Alignment) {
        val bigNumberToString = generateStringFormat(numberThatDoesntFitPadding, alignment)

        val expectedText = textBefore +
                generateIntPlaceholderWithPaddingSpaces(standardPaddingCount, alignment) +
                textAfter
        val actualText = textBefore +
                bigNumberToString +
                textAfter
        val sanitizedActualText = textBefore +
                generateIntPlaceholderWithPaddingSpaces(bigNumberToString.length, alignment) +
                textAfter
        assertExpectNotEqualsActual(expectedText, actualText, sanitizedActualText)
    }

    private fun generateIntPlaceholderWithPaddingSpaces(count: Int, alignment: NumberAgnosticSanitizer.Alignment): String {
        val placeholder = NumberAgnosticSanitizer.generatePlaceholder(NumberAgnosticSanitizer.INT_MARKER, alignment)
        return if (alignment == NumberAgnosticSanitizer.Alignment.Right) {
            buildPaddingString(count) + placeholder
        } else {
            placeholder + buildPaddingString(count)
        }
    }

    private fun generateStringFormat(number: Long, alignment: NumberAgnosticSanitizer.Alignment): String {
        val paddingFormatInfix = if (alignment == NumberAgnosticSanitizer.Alignment.Right) "" else "-"
        val paddingFormat = "%${paddingFormatInfix}${standardPaddingCount}s"
        return String.format(paddingFormat, number)
    }

    private fun buildPaddingString(count: Int): String = buildString { repeat(count) { append(' ') } }
}

private fun assertExpectEqualsActual(expected: String, actual: String) {
    assertEquals(expected, getSanitizedActual(expected, actual))
}

private fun assertExpectNotEqualsActual(expected: String, actual: String, resultActual: String) {
    val sanitizedActual = getSanitizedActual(expected, actual)
    assertFalse(expected == sanitizedActual)
    assertEquals(resultActual, sanitizedActual)
}

private fun getSanitizedActual(expectedText: String, actualText: String): String {
    val comparer = NumberAgnosticSanitizer(actualText)
    return comparer.generatedSanitizedActualTextBasedOnExpectPlaceholders(expectedText)
}


