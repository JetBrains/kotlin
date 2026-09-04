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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


interface ILineId : Comparable<ILineId> {
    val no: Int
    val generation: Int
}

data class LineId(override val no: Int, override val generation: Int, private val codeHash: Int) : ILineId, Serializable {

    override fun compareTo(other: ILineId): Int = (other as? LineId)?.let { lineId ->
        no.compareTo(lineId.no).takeIf { no -> no != 0 }
            ?: codeHash.compareTo(lineId.codeHash)
    } ?: -1

    companion object {
        private const val serialVersionUID: Long = 8328354000L
    }
}

data class ReplHistoryRecord<out T> (val id: ILineId, val item: T)

interface IReplStageHistory<T> : List<ReplHistoryRecord<T>> {

    fun peek(): ReplHistoryRecord<T>? = lock.read { lastOrNull() }

    fun push(id: ILineId, item: T)

    fun pop(): ReplHistoryRecord<T>?

    fun verifiedPop(id: ILineId): ReplHistoryRecord<T>? = lock.write {
        if (lastOrNull()?.id == id) pop()
        else null
    }

    fun reset(): Iterable<ILineId>

    fun resetTo(id: ILineId): Iterable<ILineId>

    val lock: ReentrantReadWriteLock
}

interface IReplStageState<T> {
    val history: IReplStageHistory<T>

    val lock: ReentrantReadWriteLock

    val currentGeneration: Int

    fun getNextLineNo(): Int = history.peek()?.id?.no?.let { it + 1 } ?: REPL_CODE_LINE_FIRST_NO // TODO: it should be more robust downstream (e.g. use atomic)

    @Suppress("UNCHECKED_CAST")
    fun <StateT : IReplStageState<*>> asState(target: Class<out StateT>): StateT =
        if (target.isAssignableFrom(this::class.java)) this as StateT
        else throw IllegalArgumentException("$this is not an expected instance of IReplStageState")

    fun dispose() {
    }
}

