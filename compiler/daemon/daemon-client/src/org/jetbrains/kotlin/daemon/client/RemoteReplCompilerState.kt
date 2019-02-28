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

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.common.ReplStateFacade
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

// NOTE: the lock is local
// TODO: verify that locla lock doesn't lead to any synch problems
class RemoteReplCompilerStateHistory(private val state: RemoteReplCompilerState) : IReplStageHistory<Unit>, AbstractList<ReplHistoryRecord<Unit>>() {
    override val size: Int
        get() = state.replStateFacade.getHistorySize()

    override fun get(index: Int): ReplHistoryRecord<Unit> = ReplHistoryRecord(state.replStateFacade.historyGet(index), Unit)

    override fun push(id: ILineId, item: Unit) {
        throw NotImplementedError("push to remote history is not supported")
    }

    override fun pop(): ReplHistoryRecord<Unit>? {
        throw NotImplementedError("pop from remote history is not supported")
    }

    override fun reset(): Iterable<ILineId> = state.replStateFacade.historyReset().apply {
        currentGeneration.incrementAndGet()
    }

    override fun resetTo(id: ILineId): Iterable<ILineId> = state.replStateFacade.historyResetTo(id).apply {
        currentGeneration.incrementAndGet()
    }

    val currentGeneration = AtomicInteger(REPL_CODE_LINE_FIRST_GEN)

    override val lock: ReentrantReadWriteLock get() = state.lock
}

class RemoteReplCompilerState(
    internal val replStateFacade: ReplStateFacade,
    override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageState<Unit> {

    override val currentGeneration: Int get() = (history as RemoteReplCompilerStateHistory).currentGeneration.get()

    override val history: IReplStageHistory<Unit> = RemoteReplCompilerStateHistory(this)
}