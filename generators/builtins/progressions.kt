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

import org.jetbrains.kotlin.generators.builtins.ProgressionKind
import org.jetbrains.kotlin.generators.builtins.ProgressionKind.*
import org.jetbrains.kotlin.generators.builtins.areEqualNumbers
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import org.jetbrains.kotlin.generators.builtins.hashLong
import org.jetbrains.kotlin.generators.builtins.progressionIncrementType
import java.io.PrintWriter

class GenerateProgressions(out: PrintWriter) : BuiltInsSourceGenerator(out) {

    override fun getPackage() = "kotlin.ranges"
    private fun generateDiscreteBody(kind: ProgressionKind) {
        val t = kind.capitalized
        val progression = "${t}Progression"

        val incrementType = progressionIncrementType(kind)
        fun compare(v: String) = areEqualNumbers(v)

        val zero = when (kind) {
            LONG -> "0L"
            else -> "0"
        }
        val checkZero = """if (step == $zero) throw kotlin.IllegalArgumentException("Step must be non-zero.")"""

        val stepMinValue = "$incrementType.MIN_VALUE"
        val checkMin =
            """if (step == $stepMinValue) throw kotlin.IllegalArgumentException("Step must be greater than $stepMinValue to avoid overflow on negation.")"""

        val hashCode = "=\n" + when (kind) {
            CHAR ->
                "        if (isEmpty()) -1 else (31 * (31 * first.code + last.code) + step)"
            INT ->
                "        if (isEmpty()) -1 else (31 * (31 * first + last) + step)"
            LONG ->
                "        if (isEmpty()) -1 else (31 * (31 * ${hashLong("first")} + ${hashLong("last")}) + ${hashLong("step")}).toInt()"
        }
        val elementToIncrement = when (kind) {
            CHAR -> ".code"
            else -> ""
        }
        val incrementToElement = when (kind) {
            CHAR -> ".toChar()"
            else -> ""
        }
        val one = if (kind == LONG) "1L" else "1"
        val two = if (kind == LONG) "2L" else "2"
        val incToInt =
            ".let { if (it < Int.MAX_VALUE${if (kind == LONG) ".toLong()" else ""}) it${if (kind == LONG) ".toInt()" else ""}.inc() else Int.MAX_VALUE }"
        val sizeBody = "if (isEmpty()) 0 else " +
                when (kind) {
                    CHAR -> "(last - first) / step + $one"
                    else -> """
        when {
            step == $one ->
                if (first >= $zero || last < $zero || last <= $incrementType.MAX_VALUE + first)
                    (last - first)$incToInt
                else Int.MAX_VALUE
            step == -$one ->
                if (last >= $zero || first < $zero || first <= $incrementType.MAX_VALUE + last)
                    (first - last)$incToInt
                else Int.MAX_VALUE
            step > $incrementType.MIN_VALUE / $two && step < $incrementType.MAX_VALUE / $two -> {
                //(last - first) / step =
                // = (last / step * step + last % step - first / step * step - first % step) / step =
                // = last / step - first / step + (last % step - first % step) / step

                //no overflow because |step| >= 2
                //$incrementType.MIN_VALUE / 2 <= last / step <= $incrementType.MAX_VALUE / 2
                //$incrementType.MIN_VALUE / 2 <= first / step <= $incrementType.MAX_VALUE / 2
                //$incrementType.MIN_VALUE / 2 - $incrementType.MAX_VALUE / 2 <= last / step - first / step <= $incrementType.MAX_VALUE / 2 - $incrementType.MIN_VALUE / 2
                //$incrementType.MIN_VALUE + $one <= last / step - first / step <= $incrementType.MAX_VALUE
                val div = last / step - first / step // >= 0 because either step > 0 && last >= first or step < 0 && last <= first
                //no overflow because $incrementType.MIN_VALUE / 2 < step < $incrementType.MAX_VALUE / 2
                //min($incrementType.MIN_VALUE / 2, -($incrementType.MAX_VALUE / 2)) < first % step < max($incrementType.MAX_VALUE / 2, -($incrementType.MIN_VALUE / 2))
                //$incrementType.MIN_VALUE / 2 < first % step <= $incrementType.MAX_VALUE / 2
                //$incrementType.MIN_VALUE / 2 <= first % step <= $incrementType.MAX_VALUE / 2
                //$incrementType.MIN_VALUE / 2 <= last % step <= $incrementType.MAX_VALUE / 2
                //$incrementType.MIN_VALUE <= last % step - first % step <= $incrementType.MAX_VALUE
                val rem = (last % step - first % step) / step
                if (div <= $incrementType.MAX_VALUE - rem)
                    (div + rem)$incToInt
                else
                    Int.MAX_VALUE
            }
            else -> {
                //number of items is < 5 (the smallest (by its absolute value) step is $incrementType.MAX_VALUE / 2, so if progression starts at $incrementType.MIN_VALUE, it contains 4 elements
                var count = 0
                for (item in this) count++
                count
                //count() is not used as it may recursively use size property for Collections
            }
        }""".trim()
                }

        out.println(
            """/**
 * A progression of values of type `$t`.
 */
public open class $progression
    internal constructor
    (
            start: $t,
            endInclusive: $t,
            step: $incrementType
    ) : Collection<$t> {
    init {
        $checkZero
        $checkMin
    }

    /**
     * The first element in the progression.
     */
    public val first: $t = start

    /**
     * The last element in the progression.
     */
    public val last: $t = getProgressionLastElement(start$elementToIncrement, endInclusive$elementToIncrement, step)$incrementToElement

    /**
     * The step of the progression.
     */
    public val step: $incrementType = step

    override fun iterator(): ${t}Iterator = ${t}ProgressionIterator(first, last, step)

    /**
     * Checks if the progression is empty.
     *
     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public override fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is $progression && (isEmpty() && other.isEmpty() ||
        ${compare("first")} && ${compare("last")} && ${compare("step")})

    override fun hashCode(): Int $hashCode

    override fun toString(): String = ${"if (step > 0) \"\$first..\$last step \$step\" else \"\$first downTo \$last step \${-step}\""}

    @SinceKotlin("1.6")
    override val size: Int
        get() = $sizeBody

    private infix fun $t.mod(n: $incrementType): $incrementType {
        val positiveN = kotlin.math.abs(n)
        val r = ${if (kind == CHAR) "(this - Char.MIN_VALUE)" else "this"} % positiveN
        return if (r < $zero) r + positiveN else r
    }

    @SinceKotlin("1.6")
    override fun contains(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") /* for the backward compatibility with old names */ value: $t): Boolean = when {
        step > $zero && value >= first && value <= last -> value mod step == first mod step
        step < $zero && value <= first && value >= last -> value mod step == first mod step
        else -> false
    }

    @SinceKotlin("1.6")
    override fun containsAll(elements: Collection<$t>): Boolean = if (this.isEmpty()) elements.isEmpty() else (elements as Collection<*>).all { it in this }

    companion object {
        /**
         * Creates $progression within the specified bounds of a closed range.
         *
         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `$stepMinValue` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: $t, rangeEnd: $t, step: $incrementType): $progression = $progression(rangeStart, rangeEnd, step)
    }
}"""
        )
        out.println()

    }

    override fun generateBody() {
        out.println("import kotlin.internal.getProgressionLastElement")
        out.println()
        for (kind in ProgressionKind.values()) {
            generateDiscreteBody(kind)
        }
    }
}
