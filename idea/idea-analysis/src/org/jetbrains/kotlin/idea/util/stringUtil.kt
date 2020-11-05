/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util.string

fun String.collapseSpaces(): String {
    val builder = StringBuilder()
    var haveSpaces = false
    for (c in this) {
        if (c.isWhitespace()) {
            haveSpaces = true
        } else {
            if (haveSpaces) {
                builder.append(" ")
                haveSpaces = false
            }
            builder.append(c)
        }
    }
    return builder.toString()
}

// -------------------- copied from EscapeUtils.java --------------------

private const val ESCAPE_CHAR = '\\'

private fun calcExpectedJoinedSize(list: Collection<String>) = list.size - 1 + list.sumBy { it.length }

fun Collection<String>.joinWithEscape(delimiterChar: Char): String {
    if (isEmpty()) return ""

    val expectedSize = calcExpectedJoinedSize(this)
    val out = StringBuilder(expectedSize)
    var first = true
    for (s in this) {
        if (!first) {
            out.append(delimiterChar)
        }
        first = false
        for (ch in s) {
            if (ch == delimiterChar || ch == ESCAPE_CHAR) {
                out.append(ESCAPE_CHAR)
            }
            out.append(ch)
        }
    }
    return out.toString()
}

// ----------------------------------------------------------------------