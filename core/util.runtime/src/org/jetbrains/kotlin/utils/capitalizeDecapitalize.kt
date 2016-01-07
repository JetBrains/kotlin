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

package org.jetbrains.kotlin.util.capitalizeDecapitalize

/**
 * "FooBar" -> "fooBar"
 * "FOOBar" -> "fooBar"
 * "FOO" -> "foo"
 */
fun String.decapitalizeSmart(asciiOnly: Boolean = false): String {
    fun isUpperCaseCharAt(index: Int): Boolean {
        val c = this[index]
        return if (asciiOnly) c in 'A'..'Z' else c.isUpperCase()
    }

    if (isEmpty() || !isUpperCaseCharAt(0)) return this

    if (length == 1 || !isUpperCaseCharAt(1)) {
        return if (asciiOnly) decapitalizeAsciiOnly() else decapitalize()
    }

    fun toLowerCase(string: String) = if (asciiOnly) string.toLowerCaseAsciiOnly() else string.toLowerCase()

    val secondWordStart = (indices.firstOrNull { !isUpperCaseCharAt(it) }
                           ?: return toLowerCase(this)) - 1
    return toLowerCase(substring(0, secondWordStart)) + substring(secondWordStart)
}

/**
 * "fooBar" -> "FOOBar"
 * "FooBar" -> "FOOBar"
 * "foo" -> "FOO"
 */
fun String.capitalizeFirstWord(asciiOnly: Boolean = false): String {
    fun toUpperCase(string: String) = if (asciiOnly) string.toUpperCaseAsciiOnly() else string.toUpperCase()

    fun isLowerCaseCharAt(index: Int): Boolean {
        val c = this[index]
        return if (asciiOnly) c in 'a'..'z' else c.isLowerCase()
    }

    val secondWordStart = indices.drop(1).firstOrNull { !isLowerCaseCharAt(it) }
                          ?: return toUpperCase(this)
    return toUpperCase(substring(0, secondWordStart)) + substring(secondWordStart)
}

fun String.capitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = this[0]
    return if (c in 'a'..'z')
        c.toUpperCase() + substring(1)
    else
        this
}

fun String.decapitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = this[0]
    return if (c in 'A'..'Z')
        c.toLowerCase() + substring(1)
    else
        this
}

fun String.toLowerCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'A'..'Z') c.toLowerCase() else c)
    }
    return builder.toString()
}

fun String.toUpperCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'a'..'z') c.toUpperCase() else c)
    }
    return builder.toString()
}



