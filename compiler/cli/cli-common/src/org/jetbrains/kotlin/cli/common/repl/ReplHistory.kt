/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import java.io.Serializable
import java.util.*


typealias CompiledHistoryItem<T> = Pair<CompiledReplCodeLine, T>
typealias SourceHistoryItem<T> = Pair<ReplCodeLine, T>

typealias CompiledHistoryStorage<T> = ArrayDeque<CompiledHistoryItem<T>>
typealias CompiledHistoryList<T> = List<CompiledHistoryItem<T>>
typealias SourceHistoryList<T> = List<SourceHistoryItem<T>>
typealias SourceList = List<ReplCodeLine>

/*
   WARNING: Not thread safe, the caller is assumed to lock access.
 */
class ReplHistory<T>(startingHistory: CompiledHistoryList<T> = emptyList()) : Serializable {
    private val history: CompiledHistoryStorage<T> = ArrayDeque(startingHistory)

    fun isEmpty(): Boolean = history.isEmpty()
    fun isNotEmpty(): Boolean = history.isNotEmpty()

    fun add(line: CompiledReplCodeLine, value: T) {
        history.add(line to value)
    }

    /* remove last line only if it is the line we think it is */
    fun removeLast(line: CompiledReplCodeLine): Boolean {
        return if (history.peekLast().first == line) {
            history.removeLast()
            true
        }
        else {
            false
        }
    }

    /* resets back complete history and returns the lines removed */
    fun reset(): SourceHistoryList<T> {
        val removed = history.map { Pair(it.first.source, it.second) }
        history.clear()
        return removed
    }

    /* resets back to a previous line number and returns the lines removed */
    fun resetToLine(lineNumber: Int): SourceHistoryList<T> {
        val removed = arrayListOf<SourceHistoryItem<T>>()
        while ((history.peekLast()?.first?.source?.no ?: -1) > lineNumber) {
            removed.add(history.removeLast().let { Pair(it.first.source, it.second) })
        }
        return removed.reversed()
    }

    fun resetToLine(line: ReplCodeLine): SourceHistoryList<T> = resetToLine(line.no)

    fun resetToLine(line: CompiledReplCodeLine): CompiledHistoryList<T> {
        val removed = arrayListOf<CompiledHistoryItem<T>>()
        while ((history.peekLast()?.first?.source?.no ?: -1) > line.source.no) {
            removed.add(history.removeLast())
        }
        return removed.reversed()
    }

    fun contains(line: ReplCodeLine): Boolean = history.any { it.first.source == line }
    fun contains(line: CompiledReplCodeLine): Boolean = history.any { it.first == line }

    fun lastItem(): CompiledHistoryItem<T>? = history.peekLast()
    fun lastCodeLine(): CompiledReplCodeLine? = lastItem()?.first
    fun lastValue(): T? = lastItem()?.second

    fun checkHistoryIsInSync(compareHistory: SourceList?): Boolean {
        return firstMismatchingHistory(compareHistory) == null
    }

    // return from the compareHistory the first line that does not match or null
    fun firstMismatchingHistory(compareHistory: SourceList?): Int? = when {
        compareHistory == null -> null
        compareHistory.size == history.size -> history.zip(compareHistory).firstOrNull { it.first.first.source != it.second }?.second?.no
        compareHistory.size > history.size -> compareHistory[history.size].no
        else -> history.toList()[compareHistory.size].first.source.no
    }

    fun copySources(): SourceList = history.map { it.first.source }
    fun copyValues(): List<T> = history.map { it.second }
    fun copyAll(): CompiledHistoryList<T> = history.toList()

    companion object {
        private val serialVersionUID: Long = 8328353000L
    }
}
