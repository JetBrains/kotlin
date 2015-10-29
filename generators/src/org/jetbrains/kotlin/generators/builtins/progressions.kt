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

package org.jetbrains.kotlin.generators.builtins.progressions

import org.jetbrains.kotlin.generators.builtins.*
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.*
import org.jetbrains.kotlin.generators.builtins.ProgressionKind.*
import java.io.PrintWriter

class GenerateProgressions(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun generateBody() {
        for (kind in ProgressionKind.values()) {
            val t = kind.capitalized
            val progression = "${t}Progression"

            val incrementType = progressionIncrementType(kind)

            fun compare(v: String) = areEqualNumbers(kind, v)

            val zero = when (kind) {
                FLOAT -> "0.0f"
                DOUBLE -> "0.0"
                LONG -> "0L"
                else -> "0"
            }
            val checkNaN =
                    if (kind == FLOAT || kind == DOUBLE)
                        "if (java.lang.$t.isNaN(increment)) throw IllegalArgumentException(\"Increment must be not NaN\")\n        "
                    else ""
            val checkZero = "if (increment == $zero) throw IllegalArgumentException(\"Increment must be non-zero\")"
            val constructor = checkNaN + checkZero

            val hashCode = when (kind) {
                BYTE, CHAR, SHORT -> "=\n" +
                "        if (isEmpty()) -1 else (31 * (31 * start.toInt() + endInclusive.toInt()) + increment)"
                INT -> "=\n" +
                "        if (isEmpty()) -1 else (31 * (31 * start + endInclusive) + increment)"
                LONG -> "=\n" +
                "        if (isEmpty()) -1 else (31 * (31 * ${hashLong("start")} + ${hashLong("endInclusive")}) + ${hashLong("increment")}).toInt()"
                FLOAT -> "=\n" +
                "        if (isEmpty()) -1 else (31 * (31 * ${floatToIntBits("start")} + ${floatToIntBits("endInclusive")}) + ${floatToIntBits("increment")})"
                DOUBLE -> "{\n" +
                "        if (isEmpty()) return -1\n" +
                "        var temp = ${doubleToLongBits("start")}\n" +
                "        var result = ${hashLong("temp")}\n" +
                "        temp = ${doubleToLongBits("endInclusive")}\n" +
                "        result = 31 * result + ${hashLong("temp")}\n" +
                "        temp = ${doubleToLongBits("increment")}\n" +
                "        return (31 * result + ${hashLong("temp")}).toInt()\n" +
                "    }"
            }

            if (kind == FLOAT || kind == DOUBLE) {
                out.println("""@Deprecated("This progression implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING)""")
                out.println("""@Suppress("DEPRECATION_ERROR")""")
            }

            if (kind == SHORT || kind == BYTE) {
                out.println("""@Deprecated("Use IntProgression instead.", ReplaceWith("IntProgression"), level = DeprecationLevel.WARNING)""")
            }

            out.println(
"""/**
 * A progression of values of type `$t`.
 */
public open class $progression(
        override val start: $t,
                 val endInclusive: $t,
        override val increment: $incrementType
) : Progression<$t>, Iterable<$t> {
    init {
        $constructor
    }

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    public override val end: $t get() = endInclusive

    override fun iterator(): ${t}Iterator = ${t}ProgressionIterator(start, endInclusive, increment)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (increment > 0) start > endInclusive else start < endInclusive

    override fun equals(other: Any?): Boolean =
        other is $progression && (isEmpty() && other.isEmpty() ||
        ${compare("start")} && ${compare("endInclusive")} && ${compare("increment")})

    override fun hashCode(): Int $hashCode

    override fun toString(): String = ${"if (increment > 0) \"\$start..\$endInclusive step \$increment\" else \"\$start downTo \$endInclusive step \${-increment}\""}
}""")
            out.println()
        }
    }
}
