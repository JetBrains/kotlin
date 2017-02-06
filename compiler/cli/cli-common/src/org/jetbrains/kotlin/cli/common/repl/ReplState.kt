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


interface ILineId : Comparable<ILineId> {
    val no: Int
}

data class ReplHistoryRecord<out T> (val id: ILineId, val item: T)

interface IReplStageHistory<T> : List<ReplHistoryRecord<T>> {

    fun backwardIterator(): Iterator<ReplHistoryRecord<T>> = lock.read { asReversed().iterator() } // TODO: check perf

    fun peek(): ReplHistoryRecord<T>? = lock.read { lastOrNull() }

    fun push(id: ILineId, item: T)

    fun pop(): ReplHistoryRecord<T>?

    fun verifiedPop(id: ILineId): ReplHistoryRecord<T>? = lock.write {
        if (lastOrNull()?.id == id) pop()
        else null
    }

    fun resetTo(id: ILineId): Iterable<ILineId>

    fun <OtherT> firstMismatch(other: IReplStageHistory<OtherT>): Pair<ReplHistoryRecord<T>?, ReplHistoryRecord<OtherT>?>? =
            lock.read { other.lock.read {
                iterator().asSequence().zip(other.asSequence()).firstOrNull { it.first.id != it.second.id }?.let { it.first to it.second }
            } }

    fun<OtherT> firstMismatchFiltered(other: IReplStageHistory<OtherT>, predicate: (ReplHistoryRecord<T>) -> Boolean): Pair<ReplHistoryRecord<T>?, ReplHistoryRecord<OtherT>?>? =
            lock.read { other.lock.read {
                iterator().asSequence().filter(predicate).zip(other.asSequence()).firstOrNull { it.first.id != it.second.id }?.let { it.first to it.second }
            } }

    fun<OtherT> firstMismatchWhile(other: IReplStageHistory<OtherT>, predicate: (ReplHistoryRecord<T>) -> Boolean): Pair<ReplHistoryRecord<T>?, ReplHistoryRecord<OtherT>?>? =
            lock.read { other.lock.read {
                iterator().asSequence().takeWhile(predicate).zip(other.asSequence()).firstOrNull { it.first.id != it.second.id }?.let { it.first to it.second }
            } }

    fun <OtherT> matches(other: IReplStageHistory<OtherT>): Boolean =
            lock.read { other.lock.read {
                size == other.size && firstMismatch(other) == null
            } }

    fun <OtherT> isProperPrefix(other: IReplStageHistory<OtherT>): Boolean =
            lock.read { other.lock.read {
                size == other.size - 1 && firstMismatch(other) == null
            } }

    val lock: ReentrantReadWriteLock
}

interface IReplStageState<T> {
    val history: IReplStageHistory<T>

    val lock: ReentrantReadWriteLock

    fun <StateT : IReplStageState<*>> asState(target: Class<out StateT>): StateT =
            if (target.isAssignableFrom(this::class.java)) this as StateT
            else throw IllegalArgumentException("$this is not an expected instance of IReplStageState")
}


