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

package org.jetbrains.kotlin.console

import com.intellij.openapi.util.TextRange

class CommandHistory {
    class Entry(
            val entryText: String,
            val rangeInHistoryDocument: TextRange
    )

    private val entries = arrayListOf<Entry>()
    var processedEntriesCount: Int = 0
        private set

    val listeners = arrayListOf<HistoryUpdateListener>()

    operator fun get(i: Int) = entries[i]

    fun addEntry(entry: Entry) {
        entries.add(entry)
        listeners.forEach { it.onNewEntry(entry) }
    }

    fun lastUnprocessedEntry(): CommandHistory.Entry? {
        return if (processedEntriesCount < size) {
            get(processedEntriesCount)
        }
        else {
            null
        }
    }

    fun entryProcessed() {
        processedEntriesCount++
    }

    val size: Int
        get() = entries.size
}

interface HistoryUpdateListener {
    fun onNewEntry(entry: CommandHistory.Entry)
}