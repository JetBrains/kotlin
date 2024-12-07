/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.util.capitalizeDecapitalize

/**
 * "FooBar" -> "fooBar"
 * "FOOBar" -> "fooBar"
 * "FOO" -> "foo"
 * "FOO_BAR" -> "foO_BAR"
 */
fun String.decapitalizeSmartForCompiler(asciiOnly: Boolean = false): String {
    if (isEmpty() || !isUpperCaseCharAt(0, asciiOnly)) return this

    if (length == 1 || !isUpperCaseCharAt(1, asciiOnly)) {
        return if (asciiOnly) decapitalizeAsciiOnly() else replaceFirstChar(Char::lowercaseChar)
    }

    val secondWordStart = (indices.firstOrNull { !isUpperCaseCharAt(it, asciiOnly) } ?: return toLowerCase(this, asciiOnly)) - 1

    return toLowerCase(substring(0, secondWordStart), asciiOnly) + substring(secondWordStart)
}

/**
 * "FooBar" -> "fooBar"
 * "FOOBar" -> "fooBar"
 * "FOO" -> "foo"
 * "FOO_BAR" -> "fooBar"
 * "__F_BAR" -> "fBar"
 */
fun String.decapitalizeSmart(asciiOnly: Boolean = false): String {
    return decapitalizeWithUnderscores(this, asciiOnly)
        ?: decapitalizeSmartForCompiler(asciiOnly)
}

/**
 * "fooBar" -> "FOOBar"
 * "FooBar" -> "FOOBar"
 * "foo" -> "FOO"
 */
fun String.capitalizeFirstWord(asciiOnly: Boolean = false): String {
    val secondWordStart = indices.drop(1).firstOrNull { !isLowerCaseCharAt(it, asciiOnly) }
        ?: return toUpperCase(this, asciiOnly)

    return toUpperCase(substring(0, secondWordStart), asciiOnly) + substring(secondWordStart)
}

/**
 * FOOBAR -> null
 * FOO_BAR -> "fooBar"
 * FOO_BAR_BAZ -> "fooBarBaz"
 * "__F_BAR" -> "fBar"
 * "_F_BAR" -> "fBar"
 * "F_BAR" -> "fBar"
 */
private fun decapitalizeWithUnderscores(str: String, asciiOnly: Boolean): String? {
    val words = str.split("_").filter { it.isNotEmpty() }

    if (words.size <= 1) return null

    val builder = StringBuilder()

    words.forEachIndexed { index, word ->
        if (index == 0) {
            builder.append(toLowerCase(word, asciiOnly))
        } else {
            builder.append(toUpperCase(word.first().toString(), asciiOnly))
            builder.append(toLowerCase(word.drop(1), asciiOnly))
        }
    }

    return builder.toString()
}

private fun String.isUpperCaseCharAt(index: Int, asciiOnly: Boolean): Boolean {
    val c = this[index]
    return if (asciiOnly) c in 'A'..'Z' else c.isUpperCase()
}

private fun String.isLowerCaseCharAt(index: Int, asciiOnly: Boolean): Boolean {
    val c = this[index]
    return if (asciiOnly) c in 'a'..'z' else c.isLowerCase()
}

private fun toLowerCase(string: String, asciiOnly: Boolean): String {
    return if (asciiOnly) string.toLowerCaseAsciiOnly() else string.lowercase()
}

private fun toUpperCase(string: String, asciiOnly: Boolean): String {
    return if (asciiOnly) string.toUpperCaseAsciiOnly() else string.uppercase()
}

fun String.capitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = this[0]
    return if (c in 'a'..'z')
        buildString(length) {
            append(c.uppercaseChar())
            append(this@capitalizeAsciiOnly, 1, this@capitalizeAsciiOnly.length)
        }
    else
        this
}

fun String.decapitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = this[0]
    return if (c in 'A'..'Z')
        c.lowercaseChar() + substring(1)
    else
        this
}

fun String.toLowerCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'A'..'Z') c.lowercaseChar() else c)
    }
    return builder.toString()
}

fun String.toUpperCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'a'..'z') c.uppercaseChar() else c)
    }
    return builder.toString()
}



