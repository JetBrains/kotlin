/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.ranges

import org.jetbrains.kotlin.generators.builtins.ProgressionKind
import org.jetbrains.kotlin.generators.builtins.ProgressionKind.*
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import java.io.PrintWriter

class GenerateRanges(out: PrintWriter) : BuiltInsSourceGenerator(out, annotateAsBuiltinWithBytecode = true) {
    override fun getPackage() = "kotlin.ranges"
    override fun generateBody() {
        for (kind in ProgressionKind.entries) {
            val t = kind.capitalized
            val range = "${t}Range"

            val increment = "1"

            val emptyBounds = when (kind) {
                CHAR -> "1.toChar(), 0.toChar()"
                else -> "1, 0"
            }

            val toString = "\"\$first..\$last\""

            out.println(
                """/**
 * An iterable range of values of type `$t`.
 * 
 * The range `$range` is a special case of increasing [${t}Progression] with the step equal to 1.
 * When being iterated, the range `$range` produces all values from [start] to [endInclusive].
 */
public class $range(start: $t, endInclusive: $t) : ${t}Progression(start, endInclusive, $increment), ClosedRange<$t>, OpenEndRange<$t> {
    override val start: $t get() = first
    override val endInclusive: $t get() = last
    
    @Deprecated("Can throw an exception when it's impossible to represent the value with $t type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    override val endExclusive: $t get() {
        if (last == $t.MAX_VALUE) error("Cannot return the exclusive upper bound of a range that includes MAX_VALUE.")
        return last + 1
    }

    override fun contains(value: $t): Boolean = first <= value && value <= last

    /** 
     * Checks whether the range is empty.
     *
     * The range is empty if its start value is greater than the end value.
     */
    override fun isEmpty(): Boolean = first > last
    
    /**
     * Checks if this range is equal to the specified [other] value.
     *
     * The [other] value is considered equal to `this` if [other] is [${t}Progression] 
     * and they are both [empty][isEmpty] or have the same first element, last element, and step.
     */
    override fun equals(other: Any?): Boolean = super.equals(other)

    override fun toString(): String = $toString

    public companion object {
        /** An empty range of values of type $t. */
        public val EMPTY: $range = $range($emptyBounds)
    }
}""")
            out.println()
        }
    }
}
