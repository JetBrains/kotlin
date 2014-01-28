/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.builtins.ranges

import org.jetbrains.jet.generators.builtins.*
import org.jetbrains.jet.generators.builtins.ProgressionKind.*
import java.io.PrintWriter

class GenerateRanges(val out: PrintWriter) {
    fun generate() {
        generatedBy(out)
        for (kind in ProgressionKind.values()) {
            val t = kind.capitalized
            val range = "${t}Range"

            val incrementType = progressionIncrementType(kind)

            val increment = when (kind) {
                FLOAT -> "1.0f"
                DOUBLE -> "1.0"
                else -> "1"
            }

            val emptyBounds = when (kind) {
                CHAR -> "1.toChar(), 0.toChar()"
                FLOAT -> "1.0f, 0.0f"
                DOUBLE -> "1.0, 0.0"
                else -> "1, 0"
            }

            fun compare(v: String) = areEqualNumbers(kind, v)

            val hashCode = when (kind) {
                BYTE, CHAR, SHORT -> "= 31 * start.toInt() + end"
                INT -> "= 31 * start + end"
                LONG -> "= (31 * ${hashLong("start")} + ${hashLong("end")}).toInt()"
                FLOAT -> "= 31 * ${floatToIntBits("start")} + ${floatToIntBits("end")}"
                DOUBLE -> "{\n" +
                "        var temp = ${doubleToLongBits("start")}\n" +
                "        val result = ${hashLong("temp")}\n" +
                "        temp = ${doubleToLongBits("end")}\n" +
                "        return (31 * result + ${hashLong("temp")}).toInt()\n" +
                "    }"
            }

            out.println(
"""public class $range(public override val start: $t, public override val end: $t) : Range<$t>, Progression<$t> {
    override val increment: $incrementType
        get() = $increment

    override fun contains(item: $t): Boolean = start <= item && item <= end

    override fun iterator(): ${t}Iterator = ${t}ProgressionIterator(start, end, $increment)

    fun equals(other: Any?): Boolean =
        other is $range && ${compare("start")} && ${compare("end")}

    fun hashCode(): Int $hashCode

    class object {
        public val EMPTY: $range = $range($emptyBounds)
    }
}""")
            out.println()
        }
    }
}

fun main(args: Array<String>) {
    generateBuiltInFile("Ranges.kt") {
        GenerateRanges(it).generate()
    }
}
