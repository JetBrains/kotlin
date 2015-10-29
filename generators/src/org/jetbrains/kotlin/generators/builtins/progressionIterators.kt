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
        BYTE, CHAR, SHORT -> ".toInt()" to ".to$t()"
        else -> "" to ""
    }

    return """/**
 * An iterator over a progression of values of type `$t`.
 * @property increment the number by which the value is incremented on each step.
 */
internal class ${t}ProgressionIterator(start: $t, end: $t, val increment: $incrementType) : ${t}Iterator() {
    private var next = start$toInt
    private val finalElement: $t = getProgressionFinalElement(start$toInt, end$toInt, increment)$toType
    private var hasNext: Boolean = if (increment > 0) start <= end else start >= end

    override fun hasNext(): Boolean = hasNext

    override fun next$t(): $t {
        val value = next
        if (value == finalElement$toInt) {
            hasNext = false
        }
        else {
            next += increment
        }
        return value$toType
    }
}"""
}

fun floatingPointProgressionIterator(kind: ProgressionKind): String {
    val t = kind.capitalized
    return """/**
 * An iterator over a progression of values of type `$t`.
 * @property increment the number by which the value is incremented on each step.
 */
@Deprecated("This progression implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING)
internal class ${t}ProgressionIterator(start: $t, val end: $t, val increment: $t) : ${t}Iterator() {
    private var next = start

    override fun hasNext(): Boolean = if (increment > 0) next <= end else next >= end

    override fun next$t(): $t {
        val value = next
        next += increment
        return value
    }
}"""
}

class GenerateProgressionIterators(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun generateBody() {
        out.println("import kotlin.internal.getProgressionFinalElement")
        out.println()
        for (kind in ProgressionKind.values()) {
            if (kind != FLOAT && kind != DOUBLE) {
                out.println(integerProgressionIterator(kind))
            }
            else {
                out.println(floatingPointProgressionIterator(kind))
            }
            out.println()
        }
    }
}
