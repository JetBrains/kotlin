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

    override fun getPackage() = "kotlin.ranges"
    private fun generateDiscreteBody(kind: ProgressionKind) {
        require(kind != FLOAT && kind != DOUBLE)

        val t = kind.capitalized
        val progression = "${t}Progression"

        val incrementType = progressionIncrementType(kind)
        fun compare(v: String) = areEqualNumbers(kind, v)

        val zero = when (kind) {
            LONG -> "0L"
            else -> "0"
        }
        val checkZero = "if (step == $zero) throw IllegalArgumentException(\"Step must be non-zero\")"

        val hashCode = "=\n" + when (kind) {
            BYTE, CHAR, SHORT ->
                "        if (isEmpty()) -1 else (31 * (31 * first.toInt() + last.toInt()) + step)"
            INT ->
                "        if (isEmpty()) -1 else (31 * (31 * first + last) + step)"
            LONG ->
                "        if (isEmpty()) -1 else (31 * (31 * ${hashLong("first")} + ${hashLong("last")}) + ${hashLong("step")}).toInt()"
            else -> throw IllegalArgumentException()
        }

        if (kind == SHORT || kind == BYTE) {
            out.println("""@Deprecated("Use IntProgression instead.", ReplaceWith("IntProgression"), level = DeprecationLevel.WARNING)""")
        }
        out.println(
                """/**
 * A progression of values of type `$t`.
 */
public open class $progression
    @Deprecated("This constructor will become private soon. Use $progression.fromClosedRange() instead.", ReplaceWith("$progression.fromClosedRange(start, endInclusive, step)"))
    public constructor
    (
            start: $t,
            endInclusive: $t,
            step: $incrementType
    ) : Progression<$t> /*, Iterable<$t> */ {
    init {
        $checkZero
    }

    /**
     * The first element in the progression.
     */
    public val first: $t = start

    /**
     * The last element in the progression.
     */
    public val last: $t = getProgressionLastElement(start.to$incrementType(), endInclusive.to$incrementType(), step).to$t()

    /**
     * The step of the progression.
     */
    public val step: $incrementType = step

    @Deprecated("Use 'first' property instead.", ReplaceWith("first"))
    public override val start: $t get() = first

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use 'last' property instead.")
    public override val end: $t = endInclusive

    @Deprecated("Use 'step' property instead.", ReplaceWith("step"))
    public override val increment: $incrementType get() = step


    override fun iterator(): ${t}Iterator = ${t}ProgressionIterator(first, last, step)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is $progression && (isEmpty() && other.isEmpty() ||
        ${compare("first")} && ${compare("last")} && ${compare("step")})

    override fun hashCode(): Int $hashCode

    override fun toString(): String = ${"if (step > 0) \"\$first..\$last step \$step\" else \"\$first downTo \$last step \${-step}\""}

    companion object {
        /**
         * Creates $progression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: $t, rangeEnd: $t, step: $incrementType): $progression = $progression(rangeStart, rangeEnd, step)
    }
}""")
        out.println()

    }

    private fun generateFloatingPointBody(kind: ProgressionKind) {
        require(kind == FLOAT || kind == DOUBLE)

        val t = kind.capitalized
        val progression = "${t}Progression"

        val incrementType = progressionIncrementType(kind)

        fun compare(v: String) = areEqualNumbers(kind, v)

        val zero = when (kind) {
            FLOAT -> "0.0f"
            else -> "0.0"
        }
        val constructor = "if (java.lang.$t.isNaN(increment)) throw IllegalArgumentException(\"Increment must be not NaN\")\n" +
                          "        if (increment == $zero) throw IllegalArgumentException(\"Increment must be non-zero\")"

        val hashCode = when (kind) {
            FLOAT -> "=\n" +
                     "        if (isEmpty()) -1 else (31 * (31 * ${floatToIntBits("start")} + ${floatToIntBits("endInclusive")}) + ${floatToIntBits("increment")})"
            else -> "{\n" +
                      "        if (isEmpty()) return -1\n" +
                      "        var temp = ${doubleToLongBits("start")}\n" +
                      "        var result = ${hashLong("temp")}\n" +
                      "        temp = ${doubleToLongBits("endInclusive")}\n" +
                      "        result = 31 * result + ${hashLong("temp")}\n" +
                      "        temp = ${doubleToLongBits("increment")}\n" +
                      "        return (31 * result + ${hashLong("temp")}).toInt()\n" +
                      "    }"
        }


        out.println(
                """/**
 * A progression of values of type `$t`.
 */
@Deprecated("This progression implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING)
public open class $progression(
        override val start: $t,
                 val endInclusive: $t,
        override val increment: $incrementType
) : Progression<$t> /*, Iterable<$t> */ {
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

    override fun generateBody() {
        out.println("import kotlin.internal.getProgressionLastElement")
        out.println()
        for (kind in ProgressionKind.values()) {
            if (kind == FLOAT || kind == DOUBLE)
                continue
            else
                generateDiscreteBody(kind)
        }
    }
}
