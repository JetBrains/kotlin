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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

data class LineId(override val no: Int, override val generation: Int, private val codeHash: Int) : ILineId, Serializable {

    constructor(codeLine: ReplCodeLine): this(codeLine.no, codeLine.generation, codeLine.code.hashCode())

    override fun compareTo(other: ILineId): Int = (other as? LineId)?.let {
        no.compareTo(it.no).takeIf { it != 0 }
        ?: generation.compareTo(it.generation).takeIf { it != 0 }
        ?: codeHash.compareTo(it.codeHash)
    } ?: -1 // TODO: check if it doesn't break something

    companion object {
        private val serialVersionUID: Long = 8328353000L
    }
}

open class BasicReplStageHistory<T>(override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : IReplStageHistory<T>, ArrayList<ReplHistoryRecord<T>>() {

    val currentGeneration = AtomicInteger(REPL_CODE_LINE_FIRST_GEN)

    override fun push(id: ILineId, item: T) {
        lock.write {
            add(ReplHistoryRecord(id, item))
        }
    }

    override fun pop(): ReplHistoryRecord<T>? = lock.write { if (isEmpty()) null else removeAt(lastIndex) }

    override fun reset(): Iterable<ILineId> {
        lock.write {
            val removed = map { it.id }
            clear()
            currentGeneration.incrementAndGet()
            return removed
        }
    }

    override fun resetTo(id: ILineId): Iterable<ILineId> {
        lock.write {
            return tryResetTo(id) ?: throw NoSuchElementException("Cannot reset to non-existent line ${id.no}")
        }
    }

    protected fun tryResetTo(id: ILineId): List<ILineId>? {
        val idx = indexOfFirst { it.id == id }
        if (idx < 0) return null
        return if (idx < lastIndex) {
            val removed = asSequence().drop(idx + 1).map { it.id }.toList()
            removeRange(idx + 1, size)
            currentGeneration.incrementAndGet()
            removed
        } else {
            currentGeneration.incrementAndGet()
            emptyList()
        }
    }
}

open class BasicReplStageState<HistoryItemT>(override final val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()): IReplStageState<HistoryItemT> {

    override val currentGeneration: Int get() = history.currentGeneration.get()

    override val history: BasicReplStageHistory<HistoryItemT> = BasicReplStageHistory(lock)
}
