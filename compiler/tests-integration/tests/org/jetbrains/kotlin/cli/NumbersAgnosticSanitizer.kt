/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import java.util.regex.Pattern

/**
 * Can be used for comparing data that have numbers that can be changed on each run.
 * For instance, for performance reports comparison.
 */
class NumbersAgnosticSanitizer(val actualText: String) {
    companion object {
        const val NUMBER_PLACEHOLDER_START: String = "$"
        const val NUMBER_PLACEHOLDER_END: String = "$"
        const val INT_MARKER: String = "INT"
        const val REAL_MARKER: String = "REAL"

        const val TYPE_GROUP_NAME: String = "Type"
        const val NUMBER_GROUP_NAME: String = "Number"
        const val LEFT_ALIGNMENT_MARKER: String = "<<"
        const val RIGHT_ALIGNMENT_MARKER: String = ">>"
        const val ALIGNMENT_GROUP_NAME: String = "Alignment"

        const val NUMBER_PATTERN_STRING: String = "-?\\d+(\\.\\d+)?"

        val NUMBER_PATTERN: Pattern = Pattern.compile(NUMBER_PATTERN_STRING)
        val NUMBER_PLACEHOLDER_PATTERN: Pattern = Pattern.compile(
            """($NUMBER_PLACEHOLDER_START(?<$TYPE_GROUP_NAME>$INT_MARKER|$REAL_MARKER)(?<$ALIGNMENT_GROUP_NAME>$LEFT_ALIGNMENT_MARKER|$RIGHT_ALIGNMENT_MARKER)?$NUMBER_PLACEHOLDER_END|(?<$NUMBER_GROUP_NAME>$NUMBER_PATTERN_STRING))"""
        )
    }

    val placeholdersBasedOnActualNumbers: String
    private val matchResults: List<MatchResult>

    init {
        val numberMatcherOnActual = NUMBER_PATTERN.matcher(actualText)

        matchResults = buildList {
            var previousIndex = 0
            while (numberMatcherOnActual.find()) {
                add(StringFragment(actualText.subSequence(previousIndex, numberMatcherOnActual.start())))

                val matchedNumberString = numberMatcherOnActual.group()
                val numberFragment = when {
                    matchedNumberString.contains('.') -> NumberFragment(matchedNumberString, matchedNumberString.toDouble(), NumberType.Real)
                    else -> NumberFragment(matchedNumberString, matchedNumberString.toInt(), NumberType.Int)
                }

                add(numberFragment)

                previousIndex = numberMatcherOnActual.end()
            }

            add(StringFragment(actualText.subSequence(previousIndex, actualText.length)))
        }

        placeholdersBasedOnActualNumbers = buildString {
            for (matchResult in matchResults) {
                if (matchResult is StringFragment) {
                    append(matchResult.charSequence)
                } else {
                    matchResult as NumberFragment<*>
                    append(generatePlaceholder(matchResult.numberType.value, Alignment.None))
                }
            }
        }
    }

    fun generatedSanitizedActualBasedOnExpectPlaceholders(expectedText: String): String {
        val expectPlaceholderMatcher = NUMBER_PLACEHOLDER_PATTERN.matcher(expectedText)

        return buildString {
            for (matchResult in matchResults) {
                if (matchResult is StringFragment) {
                    append(matchResult.charSequence)
                    continue
                }

                matchResult as NumberFragment<*>

                val alignment: Alignment

                val normalizedActualFragment: CharSequence
                if (expectPlaceholderMatcher.find()) {
                    alignment = expectPlaceholderMatcher.group(ALIGNMENT_GROUP_NAME)?.let {
                        if (it == LEFT_ALIGNMENT_MARKER) Alignment.Left else Alignment.Right
                    } ?: Alignment.None
                    val expectPlaceholderType = expectPlaceholderMatcher.group(TYPE_GROUP_NAME)
                    normalizedActualFragment = if (expectPlaceholderType != null) {
                        val actualIsSubtypeOfExpect = when (matchResult.numberType) {
                            NumberType.Int -> expectPlaceholderType == INT_MARKER
                            NumberType.Real -> expectPlaceholderType == REAL_MARKER
                        }
                        val numberType = if (actualIsSubtypeOfExpect)
                            expectPlaceholderType
                        else
                            matchResult.numberType.value
                        generatePlaceholder(numberType, alignment)
                    } else {
                        matchResult.charSequence
                    }
                } else {
                    alignment = Alignment.None
                    normalizedActualFragment = generatePlaceholder(matchResult.numberType.value, alignment)
                }

                if (alignment == Alignment.Left) {
                    Unit // TODO
                } else if (alignment == Alignment.Right) {
                    repeat(matchResult.charSequence.length) { append(' ') }
                    append(normalizedActualFragment)
                } else {
                    append(normalizedActualFragment)
                }
            }
        }
    }

    private fun generatePlaceholder(numberType: CharSequence, alignment: Alignment): String {
        return NUMBER_PLACEHOLDER_START +
                numberType +
                alignment.value +
                NUMBER_PLACEHOLDER_END
    }

    private sealed class MatchResult(val charSequence: CharSequence)

    private class StringFragment(charSequence: CharSequence) : MatchResult(charSequence)

    private class NumberFragment<T>(
        charSequence: CharSequence,
        val number: T,
        val numberType: NumberType,
    ) : MatchResult(charSequence)

    private enum class NumberType(val value: String) {
        Int(INT_MARKER),
        Real(REAL_MARKER),
    }

    private enum class Alignment(val value: String) {
        None(""),
        Left(LEFT_ALIGNMENT_MARKER),
        Right(RIGHT_ALIGNMENT_MARKER),
    }
}