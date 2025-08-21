/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.util.regex.Pattern

/**
 * It can be used for comparing data that have numbers that can be changed on each run (for instance, performance reports comparison).
 * Also, it supports quoted strings (single or double) to handle user-specific data (absolute project path, computer name).
 * String printing with padding is also supported, both left and right.
 * The following placeholder means that printing code uses `String.format("%8s")` with left padding (right alignment) up to 8 spaces:
 *
 * ```
 * Text:        $UINT>>$
 * ```
 *
 * It matches the following: `Text:    1234`, `Text:12345678`.
 * But `Text:1234567890` should cause a comparison failure, because typically padding is used for alignment with other lines.
 *
 * In the number of padding spaces less than 8 in expected data, comparison failure is reported as well.
 *
 * Apart from number placeholders, regular number and quoted string literals are also supported.
 *
 * See [ValueAgnosticSanitizerTest] for more detail.
 */
class ValueAgnosticSanitizer(val actualText: String) {
    companion object {
        const val PLACEHOLDER_START: String = "$"
        const val PLACEHOLDER_END: String = "$"
        const val INT_MARKER: String = "INT"
        const val UINT_MARKER: String = "UINT"
        const val REAL_MARKER: String = "REAL"
        const val QUOTED_STRING_MAKER = "QUOTEDSTRING" // No underscore, because regexes don't support them in captured group names

        const val LEFT_ALIGNMENT_MARKER: String = "<<"
        const val RIGHT_ALIGNMENT_MARKER: String = ">>"

        const val TYPE_GROUP_NAME: String = "Type"
        const val ALIGNMENT_GROUP_NAME: String = "Alignment"

        const val VALUE_PATTERN_STRING =
            """(?<$REAL_MARKER>-?(\d+\.\d*|\.\d+))|""" +
                    """(?<$INT_MARKER>-\d+)|""" +
                    """(?<$UINT_MARKER>\d+)|""" +
                    """(?<$QUOTED_STRING_MAKER>(?<QUOTE>["'])(\\.|.)*?\k<QUOTE>)""" // Matches single- and double-quoted strings. Also, it supports escaping like "\""

        val VALUE_PATTERN: Pattern = Pattern.compile(VALUE_PATTERN_STRING)
        val PLACEHOLDER_PATTERN: Pattern = Pattern.compile(
            "(" +
                    Regex.escape(PLACEHOLDER_START) +
                    "(?<$TYPE_GROUP_NAME>$INT_MARKER|$UINT_MARKER|$REAL_MARKER|$QUOTED_STRING_MAKER)" +
                    "(?<$ALIGNMENT_GROUP_NAME>$LEFT_ALIGNMENT_MARKER|$RIGHT_ALIGNMENT_MARKER)?" +
                    Regex.escape(PLACEHOLDER_END) +
                    "|" + VALUE_PATTERN_STRING + // Placeholders also match regular numbers and quoted strings
                    ")"
        )

        internal fun generatePlaceholder(numberType: CharSequence, alignment: Alignment = Alignment.None): String {
            return PLACEHOLDER_START +
                    numberType +
                    alignment.value +
                    PLACEHOLDER_END
        }
    }

    private val matchFragments: List<MatchFragment>

    init {
        val valueMatcherOnActual = VALUE_PATTERN.matcher(actualText)

        matchFragments = buildList {
            var previousIndex = 0
            while (valueMatcherOnActual.find()) {
                add(TextFragment(actualText.subSequence(previousIndex, valueMatcherOnActual.start())))

                val valueString: String
                val value: Any
                val valueType: ValueType

                val uintGroup = valueMatcherOnActual.group(UINT_MARKER)
                if (uintGroup != null) {
                    valueString = uintGroup
                    value = uintGroup.toULong()
                    valueType = ValueType.UInt
                } else {
                    var intGroup = valueMatcherOnActual.group(INT_MARKER)
                    if (intGroup != null) {
                        valueString = intGroup
                        value = intGroup.toLong()
                        valueType = ValueType.Int
                    } else {
                        val realGroup = valueMatcherOnActual.group(REAL_MARKER)
                        if (realGroup != null) {
                            valueString = realGroup
                            value = realGroup.toDouble()
                            valueType = ValueType.Real
                        } else {
                            val quotedStringGroup = valueMatcherOnActual.group(QUOTED_STRING_MAKER)
                            if (quotedStringGroup != null) {
                                valueString = quotedStringGroup
                                value = quotedStringGroup
                                valueType = ValueType.QuotedString
                            } else {
                                error("The value $valueMatcherOnActual is matched by ${VALUE_PATTERN::class.simpleName}, but it doesn't have a handler.")
                            }
                        }
                    }
                }

                add(ValueFragment(valueString, valueType, value))

                previousIndex = valueMatcherOnActual.end()
            }

            // Remember to add the trailing fragment (it could have zero lengths)
            add(TextFragment(actualText.subSequence(previousIndex, actualText.length)))
        }
    }

    /**
     * Used when an expected file is missing.
     * The function tries to recognize placeholders that are based on number or quoted string regexes.
     */
    fun generateExpectedText(): String = generateSanitizedActualTextBasedOnExpectPlaceholders("")

    /**
     * It walks by actual text fragments and sanitizes them according to expected text to avoid discrepancy in padding spaces
     * Never touch expected text for consistency, sanitize only actual text.
     */
    fun generateSanitizedActualTextBasedOnExpectPlaceholders(expectedText: String): String {
        val expectPlaceholderMatcher = PLACEHOLDER_PATTERN.matcher(expectedText)

        return buildString {
            for (matchFragment in matchFragments) {
                if (matchFragment is TextFragment) {
                    // Plain string fragments are appended without change
                    append(matchFragment.charSequence)
                    continue
                }

                require(matchFragment is ValueFragment)

                // If the expect-actual placeholder-number doesn't match, use generated placeholder from actual data
                // It's especially useful when expect data doesn't exist, but it's convenient to get it with minimal effort.
                if (!expectPlaceholderMatcher.find()) {
                    append(generatePlaceholder(matchFragment.valueType.value))
                    continue
                }

                val alignment = expectPlaceholderMatcher.group(ALIGNMENT_GROUP_NAME)?.let {
                    if (it == LEFT_ALIGNMENT_MARKER) Alignment.Left else Alignment.Right
                } ?: Alignment.None

                val expectPlaceholderType = expectPlaceholderMatcher.group(TYPE_GROUP_NAME)
                val normalizedActualFragment = if (expectPlaceholderType != null) {
                    // Match a placeholder in expected text -> extract placeholder from actual considering alignment marker
                    // It causes test data failure if placeholders don't match
                    val valueType = if (matchFragment.valueType == ValueType.UInt && expectPlaceholderType == ValueType.Int.value) {
                        // `Int` type includes `UInt` -> coerce `UInt` to `Int` in actual
                        ValueType.Int.value
                    } else {
                        matchFragment.valueType.value
                    }
                    generatePlaceholder(valueType, alignment)
                } else {
                    // Match a literal in expected data -> use plain literal from actual data
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

    private class TextFragment(charSequence: CharSequence) : MatchFragment(charSequence)

    private class ValueFragment(charSequence: CharSequence, val valueType: ValueType, val value: Any) : MatchFragment(charSequence)

    private enum class ValueType(val value: String) {
        Int(INT_MARKER),
        UInt(UINT_MARKER),
        Real(REAL_MARKER),
        QuotedString(QUOTED_STRING_MAKER),
    }

    enum class Alignment(val value: String) {
        None(""),
        Left(LEFT_ALIGNMENT_MARKER),
        Right(RIGHT_ALIGNMENT_MARKER),
    }
}