/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import java.util.regex.Pattern

/**
 * It can be used for comparing data that have numbers that can be changed on each run (for instance, performance reports comparison).
 * String printing with padding is also supported, both left and right.
 * The following placeholder means that printing code uses `String.format("%8s")` with left padding (right alignment) up to 8 spaces:
 *
 * ```
 * Text:        $INT>>$
 * ```
 *
 * It matches the following: `Text:    1234`, `Text:12345678`.
 * But `Text:1234567890` should cause a comparison failure, because typically padding is used for alignment with other lines.
 *
 * In the number of padding spaces less than 8 in expected data, comparison failure is reported as well.
 *
 * Apart from number placeholders, regular number literals are also supported.
 *
 * See [NumberAgnosticSanitizerTest] for more detail.
 */
class NumberAgnosticSanitizer(val actualText: String) {
    companion object {
        const val NUMBER_PLACEHOLDER_START: String = "$"
        const val NUMBER_PLACEHOLDER_END: String = "$"
        const val INT_MARKER: String = "INT"
        const val REAL_MARKER: String = "REAL"

        const val LEFT_ALIGNMENT_MARKER: String = "<<"
        const val RIGHT_ALIGNMENT_MARKER: String = ">>"

        const val TYPE_GROUP_NAME: String = "Type"
        const val NUMBER_GROUP_NAME: String = "Number"
        const val ALIGNMENT_GROUP_NAME: String = "Alignment"

        const val NUMBER_PATTERN_STRING: String = """-?(\d+(\.\d*)?|\.\d+)"""

        val NUMBER_PATTERN: Pattern = Pattern.compile(NUMBER_PATTERN_STRING)
        val NUMBER_PLACEHOLDER_PATTERN: Pattern = Pattern.compile(
            "(" +
                    Regex.escape(NUMBER_PLACEHOLDER_START) +
                    "(?<$TYPE_GROUP_NAME>$INT_MARKER|$REAL_MARKER)" +
                    "(?<$ALIGNMENT_GROUP_NAME>$LEFT_ALIGNMENT_MARKER|$RIGHT_ALIGNMENT_MARKER)?" +
                    Regex.escape(NUMBER_PLACEHOLDER_END) +
                    "|" +
                    "(?<$NUMBER_GROUP_NAME>$NUMBER_PATTERN_STRING)" + // Number placeholder also matches regular numbers
                    ")"
        )

        internal fun generatePlaceholder(numberType: CharSequence, alignment: Alignment = Alignment.None): String {
            return NUMBER_PLACEHOLDER_START +
                    numberType +
                    alignment.value +
                    NUMBER_PLACEHOLDER_END
        }
    }

    private val matchFragments: List<MatchFragment>

    init {
        val numberMatcherOnActual = NUMBER_PATTERN.matcher(actualText)

        matchFragments = buildList {
            var previousIndex = 0
            while (numberMatcherOnActual.find()) {
                add(StringFragment(actualText.subSequence(previousIndex, numberMatcherOnActual.start())))

                val matchedNumberString = numberMatcherOnActual.group()
                val numberType = when {
                    matchedNumberString.contains('.') -> {
                        matchedNumberString.toDouble() // Make sure it's correct numbers that doesn't exceed maximal value
                        NumberType.Real
                    }
                    else -> {
                        matchedNumberString.toLong() // Make sure it's correct numbers that doesn't exceed maximal value
                        NumberType.Int
                    }
                }

                add(NumberFragment(matchedNumberString, numberType))

                previousIndex = numberMatcherOnActual.end()
            }

            // Remember to add the trailing fragment (it could have zero lengths)
            add(StringFragment(actualText.subSequence(previousIndex, actualText.length)))
        }
    }

    fun generateExpectedTextBasedOnActualNumbers(): String = generatedSanitizedActualTextBasedOnExpectPlaceholders("")

    /**
     * It walks by actual text fragments and sanitizes them according to expected text to avoid discrepancy in padding spaces
     * Never touch expected text for consistency, sanitize only actual text.
     */
    fun generatedSanitizedActualTextBasedOnExpectPlaceholders(expectedText: String): String {
        val expectPlaceholderMatcher = NUMBER_PLACEHOLDER_PATTERN.matcher(expectedText)

        return buildString {
            for (matchFragment in matchFragments) {
                if (matchFragment is StringFragment) {
                    // Plain string fragments are appended without change
                    append(matchFragment.charSequence)
                    continue
                }

                require(matchFragment is NumberFragment)

                // If expect-actual placeholder-number doesn't match, use generated placeholder from actual data
                // It's especially useful when expect data doesn't exist, but it's convenient to get it with minimal effort.
                if (!expectPlaceholderMatcher.find()) {
                    append(generatePlaceholder(matchFragment.numberType.value))
                    continue
                }

                val alignment = expectPlaceholderMatcher.group(ALIGNMENT_GROUP_NAME)?.let {
                    if (it == LEFT_ALIGNMENT_MARKER) Alignment.Left else Alignment.Right
                } ?: Alignment.None

                val expectPlaceholderType = expectPlaceholderMatcher.group(TYPE_GROUP_NAME)
                val normalizedActualFragment = if (expectPlaceholderType != null) {
                    // Match a placeholder in expected text -> extract placeholder from actual considering alignment marker
                    // It causes test data failure if placeholders don't match
                    generatePlaceholder(matchFragment.numberType.value, alignment)
                } else {
                    // Match a number literal in expected data -> use plain literal from actual data
                    matchFragment.charSequence
                }

                // Append padding spaces if needed
                when (alignment) {
                    Alignment.Left -> {
                        append(normalizedActualFragment)
                        repeat(matchFragment.charSequence.length) { append(' ') }
                    }
                    Alignment.Right -> {
                        repeat(matchFragment.charSequence.length) { append(' ') }
                        append(normalizedActualFragment)
                    }
                    Alignment.None -> {
                        append(normalizedActualFragment)
                    }
                }
            }
        }
    }

    private sealed class MatchFragment(val charSequence: CharSequence) {
        override fun toString(): String = charSequence.toString()
    }

    private class StringFragment(charSequence: CharSequence) : MatchFragment(charSequence)

    private class NumberFragment(charSequence: CharSequence, val numberType: NumberType) : MatchFragment(charSequence)

    private enum class NumberType(val value: String) {
        Int(INT_MARKER),
        Real(REAL_MARKER),
    }

    enum class Alignment(val value: String) {
        None(""),
        Left(LEFT_ALIGNMENT_MARKER),
        Right(RIGHT_ALIGNMENT_MARKER),
    }
}