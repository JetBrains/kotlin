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

package org.jetbrains.kotlin.idea.util.string

fun String.collapseSpaces(): String {
    val builder = StringBuilder()
    var haveSpaces = false
    for (c in this) {
        if (c.isWhitespace()) {
            haveSpaces = true
        }
        else {
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

private val ESCAPE_CHAR = '\\'

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
        for (i in 0 until s.length) {
            val ch = s[i]
            if (ch == delimiterChar || ch == ESCAPE_CHAR) {
                out.append(ESCAPE_CHAR)
            }
            out.append(ch)
        }
    }
    return out.toString()
}

// ----------------------------------------------------------------------