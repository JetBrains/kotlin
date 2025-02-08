/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse

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

    fun testNumberLiterals() {
        assertExpectEqualsActual("42 0.53", "42 0.53")
    }

    fun testMismatchedNumberLiterals() {
        assertExpectNotEqualsActual("5 32.0", "43 0.85", "43 0.85")
    }

    fun testEmptyExpect() {
        // Generate placeholders on an empty expected file
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
        checkAlignmentTypical(NumberAgnosticComparer.Alignment.Right)
    }

    // Expect template is incorrect -> it requires increasing the number of spaces left to $INT$ placeholder (to 8)
    fun testRightAlignmentWhenPaddingSpacesInExpectLessThanCorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectSmallPaddingInExpect, NumberAgnosticComparer.Alignment.Right)
    }

    fun testRightAlignmentWhenPaddingSpacesInExpectGreaterThanCorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectBigPaddingInExpect, NumberAgnosticComparer.Alignment.Right)
    }

    // a value exceeds padding count -> it requires increasing the number of padding spaces in formatting code (11 instead of 8)
    fun testRightAlignmentWhenPrintedStringExceedsPaddingSpaces() {
        checkAlignmentWhenPrintedStringExceedPaddingSpaces(NumberAgnosticComparer.Alignment.Right)
    }

    fun testRightAlignmentWhenExpectAlignmentMarkerIsIncorrect() {
        checkAlignmentWhenIncorrectMarkerInExpect(NumberAgnosticComparer.Alignment.Right)
    }

    // Typical case: a value fits padding count
    fun testLeftAlignmentTypical() {
        checkAlignmentTypical(NumberAgnosticComparer.Alignment.Left)
    }

    // Expect template is incorrect -> it requires increasing the number of spaces left to $INT$ placeholder (to 8)
    fun testLeftAlignmentWhenPaddingSpacesInExpectLessThanCorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectSmallPaddingInExpect, NumberAgnosticComparer.Alignment.Left)
    }

    fun testLeftAlignmentWhenPaddingSpacesInExpectGreaterThanCorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectBigPaddingInExpect, NumberAgnosticComparer.Alignment.Left)
    }

    // a value exceeds padding count -> it requires increasing the number of padding spaces in formatting code (11 instead of 8)
    fun testLeftAlignmentWhenPrintedStringExceedsPaddingSpaces() {
        checkAlignmentWhenPrintedStringExceedPaddingSpaces(NumberAgnosticComparer.Alignment.Left)
    }

    fun testLeftAlignmentWhenExpectAlignmentMarkerIsIncorrect() {
        checkAlignmentWhenIncorrectMarkerInExpect(NumberAgnosticComparer.Alignment.Left)
    }

    private fun checkAlignmentTypical(alignment: NumberAgnosticComparer.Alignment) {
        val expectedText =
            textBefore + generateIntPlaceholderWithPaddingSpaces(standardPaddingCount, alignment) + textAfter
        val actualNormalText =
            textBefore + generateStringFormat(numberThatFitsPadding, alignment) + textAfter
        assertExpectEqualsActual(expectedText, actualNormalText)
    }

    private fun checkAlignmentWhenIncorrectMarkerInExpect(alignment: NumberAgnosticComparer.Alignment) {
        val oppositeAlignment = if (alignment == NumberAgnosticComparer.Alignment.Right)
            NumberAgnosticComparer.Alignment.Left
        else
            NumberAgnosticComparer.Alignment.Right
        val sanitizedPlaceholderLeftPaddingLength: Int
        val sanitizedPlaceholderRightPaddingLength: Int
        val numberToString = numberThatFitsPadding.toString()
        if (alignment == NumberAgnosticComparer.Alignment.Right) {
            sanitizedPlaceholderLeftPaddingLength = standardPaddingCount - numberToString.length
            sanitizedPlaceholderRightPaddingLength = numberToString.length
        } else {
            sanitizedPlaceholderLeftPaddingLength = numberToString.length
            sanitizedPlaceholderRightPaddingLength = standardPaddingCount - numberToString.length
        }
        val expectedText = textBefore +
                generateIntPlaceholderWithPaddingSpaces(standardPaddingCount, oppositeAlignment) +
                textAfter
        val actualNormalText = textBefore +
                generateStringFormat(numberThatFitsPadding, alignment) +
                textAfter
        val sanitizedActualText = textBefore +
                buildPaddingString(sanitizedPlaceholderLeftPaddingLength) +
                NumberAgnosticComparer.generatePlaceholder(NumberAgnosticComparer.INT_MARKER, oppositeAlignment) +
                buildPaddingString(sanitizedPlaceholderRightPaddingLength) +
                textAfter
        assertExpectNotEqualsActual(expectedText, actualNormalText, sanitizedActualText)
    }

    private fun checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectPaddingCountInExpect: Int, alignment: NumberAgnosticComparer.Alignment) {
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

    private fun checkAlignmentWhenPrintedStringExceedPaddingSpaces(alignment: NumberAgnosticComparer.Alignment) {
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
        assertExpectNotEqualsActual(
            expectedText,
            actualText,
            sanitizedActualText
        )
    }

    private fun generateIntPlaceholderWithPaddingSpaces(count: Int, alignment: NumberAgnosticComparer.Alignment): String {
        val placeholder = NumberAgnosticComparer.generatePlaceholder(NumberAgnosticComparer.INT_MARKER, alignment)
        return if (alignment == NumberAgnosticComparer.Alignment.Right) {
            buildPaddingString(count) + placeholder
        } else {
            placeholder + buildPaddingString(count)
        }
    }

    private fun generateStringFormat(number: Long, alignment: NumberAgnosticComparer.Alignment): String {
        val paddingFormatInfix = if (alignment == NumberAgnosticComparer.Alignment.Right) "" else "-"
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
    val comparer = NumberAgnosticComparer(actualText)
    return comparer.generatedSanitizedActualTextBasedOnExpectPlaceholders(expectedText)
}


