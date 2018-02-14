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

package org.jetbrains.kotlin.generators.builtins.progressionIterators

import org.jetbrains.kotlin.generators.builtins.*
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.*
import org.jetbrains.kotlin.generators.builtins.ProgressionKind.*
import java.io.PrintWriter

fun integerProgressionIterator(kind: ProgressionKind): String {
    val t = kind.capitalized

    val incrementType = progressionIncrementType(kind)

    val (toInt, toType) = when (kind) {
        CHAR -> ".toInt()" to ".to$t()"
        else -> "" to ""
    }

    return """/**
 * An iterator over a progression of values of type `$t`.
 * @property step the number by which the value is incremented on each step.
 */
internal class ${t}ProgressionIterator(first: $t, last: $t, val step: $incrementType) : ${t}Iterator() {
    private val finalElement = last$toInt
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private var next = if (hasNext) first$toInt else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next$t(): $t {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        }
        else {
            next += step
        }
        return value$toType
    }
}"""
}


class GenerateProgressionIterators(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = "kotlin.ranges"
    override fun generateBody() {
        for (kind in ProgressionKind.values()) {
            out.println(integerProgressionIterator(kind))
            out.println()
        }
    }
}
