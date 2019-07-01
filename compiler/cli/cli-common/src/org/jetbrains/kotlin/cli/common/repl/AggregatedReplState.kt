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

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

open class AggregatedReplStateHistory<T1, T2>(
        private val history1: IReplStageHistory<T1>,
        private val history2: IReplStageHistory<T2>,
        override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageHistory<Pair<T1, T2>>, AbstractList<ReplHistoryRecord<Pair<T1, T2>>>()
{
    override val size: Int
        get() = minOf(history1.size, history2.size)

    override fun push(id: ILineId, item: Pair<T1, T2>) {
        lock.write {
            assertSameSize()
            history1.push(id, item.first)
            history2.push(id, item.second)
        }
    }

    override fun get(index: Int): ReplHistoryRecord<Pair<T1, T2>> = lock.read {
        assertSameSize()
        val r1 = history1[index]
        val r2 = history2[index]
        assertSameId(r1, r2)
        ReplHistoryRecord(r1.id, r1.item to r2.item)
    }

    override fun pop(): ReplHistoryRecord<Pair<T1, T2>>? = lock.write {
        assertSameSize()
        val r1 = history1.pop()
        val r2 = history2.pop()
        if (r1 == null && r2 == null) return null
        if (r1 == null || r2 == null) throw IllegalStateException("Aggregated history mismatch: $r1 vs $r2")
        assertSameId(r1, r2)
        ReplHistoryRecord(r1.id, r1.item to r2.item)
    }

    override fun reset(): Iterable<ILineId> = lock.write {
        assertSameSize()
        val i1 = history1.reset().toList()
        val i2 = history2.reset().toList()
        if (i1 != i2) throw IllegalStateException("Aggregated history reset lines mismatch: $i1 != $i2")
        i1
    }

    override fun resetTo(id: ILineId): Iterable<ILineId> = lock.write {
        assertSameSize()
        val i1 = history1.resetTo(id).toList()
        val i2 = history2.resetTo(id).toList()
        if (i1 != i2) throw IllegalStateException("Aggregated history reset lines mismatch: $i1 != $i2")
        i1
    }

    private fun assertSameSize() {
        if (history1.size != history2.size) throw IllegalStateException("Aggregated history sizes mismatch: ${history1.size} != ${history2.size}")
    }

    private fun assertSameId(r1: ReplHistoryRecord<T1>, r2: ReplHistoryRecord<T2>) {
        if (r1.id != r2.id) throw IllegalStateException("Aggregated history mismatch: ${r1.id} != ${r2.id}")
    }
}

open class AggregatedReplStageState<T1, T2>(val state1: IReplStageState<T1>, val state2: IReplStageState<T2>, final override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock())
    : IReplStageState<Pair<T1, T2>>
{
    override val history: IReplStageHistory<Pair<T1, T2>> = AggregatedReplStateHistory(state1.history, state2.history, lock)

    override fun <StateT : IReplStageState<*>> asState(target: Class<out StateT>): StateT =
        @Suppress("UNCHECKED_CAST")
        when {
            target.isAssignableFrom(state1::class.java) -> state1 as StateT
            target.isAssignableFrom(state2::class.java) -> state2 as StateT
            else -> super.asState(target)
        }

    override fun getNextLineNo() = state1.getNextLineNo()

    override val currentGeneration: Int get() = state1.currentGeneration

    override fun dispose() {
        state2.dispose()
        state1.dispose()
        super.dispose()
    }
}

