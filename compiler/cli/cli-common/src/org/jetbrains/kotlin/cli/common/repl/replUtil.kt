/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common.repl

import com.google.common.base.Throwables
import java.io.Serializable

// TODO: thread safety!!
data class ReplHistory<T>(
        val lines: MutableList<ReplCodeLine> = arrayListOf(),
        val values: MutableList<T> = arrayListOf()
) : Serializable
{
    init { assert(isValid()) }
    fun isValid() = lines.size == values.size

    fun add(line: ReplCodeLine, value: T) {
        lines.add(line)
        values.add(value)
    }

    fun trimAt(idx: Int) {
        lines.dropLast(lines.size - idx)
        values.dropLast(lines.size - idx)
    }

    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

fun <T> checkAndUpdateReplHistoryCollection(history: ReplHistory<T>, linesHistory: Iterable<ReplCodeLine>): Int? {
    assert(history.isValid())
    val linesHistoryIt = linesHistory.iterator()
    var idx = 0
    while (linesHistoryIt.hasNext()) {
        val curLine = linesHistoryIt.next()
        if (history.lines[idx] != curLine) return curLine.no
        idx += 1
    }
    history.trimAt(idx)
    return null
}

fun renderReplStackTrace(cause: Throwable, startFromMethodName: String): String {
    val newTrace = arrayListOf<StackTraceElement>()
    var skip = true
    for ((i, element) in cause.stackTrace.withIndex().reversed()) {
        if ("${element.className}.${element.methodName}" == startFromMethodName) {
            skip = false
        }
        if (!skip) {
            newTrace.add(element)
        }
    }

    val resultingTrace = newTrace.reversed().dropLast(1)

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UsePropertyAccessSyntax")
    (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())

    return Throwables.getStackTraceAsString(cause)
}

