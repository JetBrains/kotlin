/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.builtins.progressions

import org.jetbrains.jet.generators.builtins.*
import org.jetbrains.jet.generators.builtins.generateBuiltIns.*
import org.jetbrains.jet.generators.builtins.ProgressionKind.*
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
                BYTE, CHAR, SHORT -> "= 31 * (31 * start.toInt() + end) + increment"
                INT -> "= 31 * (31 * start + end) + increment"
                LONG -> "= (31 * (31 * ${hashLong("start")} + ${hashLong("end")}) + ${hashLong("increment")}).toInt()"
                FLOAT -> "= 31 * (31 * ${floatToIntBits("start")} + ${floatToIntBits("end")}) + ${floatToIntBits("increment")}"
                DOUBLE -> "{\n" +
                "        var temp = ${doubleToLongBits("start")}\n" +
                "        var result = ${hashLong("temp")}\n" +
                "        temp = ${doubleToLongBits("end")}\n" +
                "        result = 31 * result + ${hashLong("temp")}\n" +
                "        temp = ${doubleToLongBits("increment")}\n" +
                "        return (31 * result + ${hashLong("temp")}).toInt()\n" +
                "    }"
            }

            out.println(
"""public class $progression(
        public override val start: $t,
        public override val end: $t,
        public override val increment: $incrementType
) : Progression<$t> {
    {
        $constructor
    }

    override fun iterator(): ${t}Iterator = ${t}ProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is $progression && ${compare("start")} && ${compare("end")} && ${compare("increment")}

    override fun hashCode(): Int $hashCode

    override fun toString(): String = ${"if (increment > 0) \"\$start..\$end step \$increment\" else \"\$start downTo \$end step \${-increment}\""}
}""")
            out.println()
        }
    }
}
