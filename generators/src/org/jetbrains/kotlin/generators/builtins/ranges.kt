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

package org.jetbrains.kotlin.generators.builtins.ranges

import org.jetbrains.kotlin.generators.builtins.*
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.*
import org.jetbrains.kotlin.generators.builtins.ProgressionKind.*
import java.io.PrintWriter

class GenerateRanges(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = "kotlin.ranges"
    override fun generateBody() {
        for (kind in ProgressionKind.values()) {
            val t = kind.capitalized
            val range = "${t}Range"

            val increment = "1"

            val emptyBounds = when (kind) {
                CHAR -> "1.toChar(), 0.toChar()"
                else -> "1, 0"
            }

            fun compare(v: String) = areEqualNumbers(v)

            val hashCode = when (kind) {
                CHAR -> "=\n" +
                "        if (isEmpty()) -1 else (31 * first.toInt() + last.toInt())"
                INT -> "=\n" +
                "        if (isEmpty()) -1 else (31 * first + last)"
                LONG -> "=\n" +
                "        if (isEmpty()) -1 else (31 * ${hashLong("first")} + ${hashLong("last")}).toInt()"
            }

            val toString = "\"\$first..\$last\""

            out.println(
"""/**
 * A range of values of type `$t`.
 */
public class $range(start: $t, endInclusive: $t) : ${t}Progression(start, endInclusive, $increment), ClosedRange<$t> {
    override val start: $t get() = first
    override val endInclusive: $t get() = last

    override fun contains(value: $t): Boolean = first <= value && value <= last

    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is $range && (isEmpty() && other.isEmpty() ||
        ${compare("first")} && ${compare("last")})

    override fun hashCode(): Int $hashCode

    override fun toString(): String = $toString

    companion object {
        /** An empty range of values of type $t. */
        public val EMPTY: $range = $range($emptyBounds)
    }
}""")
            out.println()
        }
    }
}
