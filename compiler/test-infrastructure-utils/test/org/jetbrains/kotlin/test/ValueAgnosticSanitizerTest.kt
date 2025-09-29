/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ValueAgnosticSanitizerTest {
    companion object {
        private val INT = ValueAgnosticSanitizer.generatePlaceholder(ValueAgnosticSanitizer.INT_MARKER)
        private val UINT = ValueAgnosticSanitizer.generatePlaceholder(ValueAgnosticSanitizer.UINT_MARKER)
        private val REAL = ValueAgnosticSanitizer.generatePlaceholder(ValueAgnosticSanitizer.REAL_MARKER)
        private val QUOTED_STRING = ValueAgnosticSanitizer.generatePlaceholder(ValueAgnosticSanitizer.QUOTED_STRING_MAKER)
    }

    @Test
    fun testPlaceholders() {
        assertExpectEqualsActual(
            "$UINT   $UINT   $INT   $REAL   $REAL   $REAL   $QUOTED_STRING   $QUOTED_STRING",
            """0   123   -65   45.67   .89   -0.9   "a\"b c"   'd\'e f'"""
        )
    }

    @Test
    fun testMismatchedTypes() {
        assertExpectNotEqualsActual(
            "$INT $UINT $REAL $INT $UINT ",
            """0.25 -54 54 'str' "str2" """,
            "$REAL $INT $UINT $QUOTED_STRING $QUOTED_STRING "
        )
    }

    @Test
    fun testLiterals() {
        assertExpectEqualsActual(
            """42 0.53 -67 "a\"b" 'c\'d'""",
            """42 0.53 -67 "a\"b" 'c\'d'"""
        )
    }

    @Test
    fun testMismatchedLiterals() {
        assertExpectNotEqualsActual(
            """5 32.0 -7 "expected_str1" 'expected_str2' 'single_quoted_str' """,
            """43 0.85 -8 "actual_str1" 'actual_str2' "double_quoted_str" """,
            """43 0.85 -8 "actual_str1" 'actual_str2' "double_quoted_str" """,
        )
    }

    @Test
    fun testEmptyExpect() {
        // Generate placeholders on nonexisting/empty expected file
        assertExpectNotEqualsActual(
            "",
            """1234 56.68 -123 'str' "str2" """,
            "$UINT $REAL $INT $QUOTED_STRING $QUOTED_STRING "
        )
    }

    @Test
    fun testUIntInActualMatchesIntInExpect() {
        assertExpectEqualsActual(INT, "123")
    }

    @Test
    fun testSingleQuotedStringDoesntMatchDoubleQuotedString() {
        assertExpectNotEqualsActual(
            """ 'single_quoted_str' """,
            """ "double_quoted_str" """,
            """ "double_quoted_str" """,
        )
    }

    @Test
    fun testMixedQuotesInStringLiterals() {
        assertExpectNotEqualsActual(
            """ $QUOTED_STRING """,
            """ "str_with_mixed_quotes' """,
            """ "str_with_mixed_quotes' """,
        )
    }

    @Test
    fun testQuotedStringMatchingIsNotGreedy() {
        assertExpectEqualsActual(""" $QUOTED_STRING: $QUOTED_STRING """, """ 'kind': 'string' """)
        assertExpectEqualsActual(""" $QUOTED_STRING: $QUOTED_STRING """, """ "kind": "string" """)
    }

    @Test
    fun testMismatchedPlaceholdersAndLiterals() {
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
            $UINT mismatched_number
            last_line
            """.trimIndent()
        )
    }
}

class NumberAgnosticComparerAlignmentTest {
    private val standardPaddingCount = 8
    private val numberThatFitsPadding = 123L
    private val numberThatDoesntFitPadding = 12345678901L
    private val textBefore = "text_before:"
    private val textAfter = ":text_after"
    private val incorrectSmallPaddingInExpect = 4
    private val incorrectBigPaddingInExpect = 12

    // Typical case: a value fits padding count
    @Test
    fun testRightAlignmentTypical() {
        checkAlignmentTypical(ValueAgnosticSanitizer.Alignment.Right)
    }

    // Expect template is incorrect -> it requires increasing/decreasing the number of spaces left to $UINT$ placeholder
    @Test
    fun testRightAlignmentWhenPaddingSpacesInExpectAreIncorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectSmallPaddingInExpect, ValueAgnosticSanitizer.Alignment.Right)
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectBigPaddingInExpect, ValueAgnosticSanitizer.Alignment.Right)
    }

    // A value exceeds padding count -> it requires increasing the number of padding spaces in code that prints this string
    @Test
    fun testRightAlignmentWhenPrintedStringExceedsPaddingSpaces() {
        checkAlignmentWhenPrintedStringExceedPaddingSpaces(ValueAgnosticSanitizer.Alignment.Right)
    }

    // Incorrect marker in expect template -> it requires fixing the marker
    @Test
    fun testRightAlignmentWhenExpectAlignmentMarkerIsIncorrect() {
        checkAlignmentWhenIncorrectMarkerInExpect(ValueAgnosticSanitizer.Alignment.Right, ValueAgnosticSanitizer.Alignment.Left)
        checkAlignmentWhenIncorrectMarkerInExpect(ValueAgnosticSanitizer.Alignment.Right, ValueAgnosticSanitizer.Alignment.None)
    }

    // Typical case: a value fits padding count
    @Test
    fun testLeftAlignmentTypical() {
        checkAlignmentTypical(ValueAgnosticSanitizer.Alignment.Left)
    }

    // Expect template is incorrect -> it requires increasing/decreasing the number of spaces right to $UINT$ placeholder
    @Test
    fun testLeftAlignmentWhenPaddingSpacesInExpectAreIncorrect() {
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectSmallPaddingInExpect, ValueAgnosticSanitizer.Alignment.Left)
        checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectBigPaddingInExpect, ValueAgnosticSanitizer.Alignment.Left)
    }

    // A value exceeds padding count -> it requires increasing the number of padding spaces in code that prints this string
    @Test
    fun testLeftAlignmentWhenPrintedStringExceedsPaddingSpaces() {
        checkAlignmentWhenPrintedStringExceedPaddingSpaces(ValueAgnosticSanitizer.Alignment.Left)
    }

    // Incorrect marker in expect template -> it requires fixing the marker
    @Test
    fun testLeftAlignmentWhenExpectAlignmentMarkerIsIncorrect() {
        checkAlignmentWhenIncorrectMarkerInExpect(ValueAgnosticSanitizer.Alignment.Left, ValueAgnosticSanitizer.Alignment.Right)
        checkAlignmentWhenIncorrectMarkerInExpect(ValueAgnosticSanitizer.Alignment.Left, ValueAgnosticSanitizer.Alignment.None)
    }

    private fun checkAlignmentTypical(alignment: ValueAgnosticSanitizer.Alignment) {
        val expectedText =
            textBefore + generateIntPlaceholderWithPaddingSpaces(standardPaddingCount, alignment) + textAfter
        val actualNormalText =
            textBefore + generateStringFormat(numberThatFitsPadding, alignment) + textAfter
        assertExpectEqualsActual(expectedText, actualNormalText)
    }

    private fun checkAlignmentWhenIncorrectMarkerInExpect(
        alignment: ValueAgnosticSanitizer.Alignment,
        incorrectAlignment: ValueAgnosticSanitizer.Alignment
    ) {
        assert(alignment != incorrectAlignment)
        val sanitizedPlaceholderLeftPaddingLength: Int
        val sanitizedPlaceholderRightPaddingLength: Int
        val numberToString = numberThatFitsPadding.toString()
        if (alignment == ValueAgnosticSanitizer.Alignment.Right) {
            sanitizedPlaceholderLeftPaddingLength = standardPaddingCount - numberToString.length
            sanitizedPlaceholderRightPaddingLength =
                if (incorrectAlignment == ValueAgnosticSanitizer.Alignment.Left) numberToString.length else 0
        } else {
            sanitizedPlaceholderLeftPaddingLength =
                if (incorrectAlignment == ValueAgnosticSanitizer.Alignment.Right) numberToString.length else 0
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
                ValueAgnosticSanitizer.generatePlaceholder(ValueAgnosticSanitizer.UINT_MARKER, incorrectAlignment) +
                buildPaddingString(sanitizedPlaceholderRightPaddingLength) +
                textAfter
        assertExpectNotEqualsActual(expectedText, actualNormalText, sanitizedActualText)
    }

    private fun checkAlignmentWhenPaddingSpacesInExpectIsIncorrect(incorrectPaddingCountInExpect: Int, alignment: ValueAgnosticSanitizer.Alignment) {
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

    private fun checkAlignmentWhenPrintedStringExceedPaddingSpaces(alignment: ValueAgnosticSanitizer.Alignment) {
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

    private fun generateIntPlaceholderWithPaddingSpaces(count: Int, alignment: ValueAgnosticSanitizer.Alignment): String {
        val placeholder = ValueAgnosticSanitizer.generatePlaceholder(ValueAgnosticSanitizer.UINT_MARKER, alignment)
        return if (alignment == ValueAgnosticSanitizer.Alignment.Right) {
            buildPaddingString(count) + placeholder
        } else {
            placeholder + buildPaddingString(count)
        }
    }

    private fun generateStringFormat(number: Long, alignment: ValueAgnosticSanitizer.Alignment): String {
        val paddingFormatInfix = if (alignment == ValueAgnosticSanitizer.Alignment.Right) "" else "-"
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
    val comparer = ValueAgnosticSanitizer(actualText)
    return comparer.generateSanitizedActualTextBasedOnExpectPlaceholders(expectedText)
}
