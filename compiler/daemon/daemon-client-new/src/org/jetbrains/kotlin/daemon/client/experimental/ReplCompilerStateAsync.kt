/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.client.RemoteReplCompilerStateHistory
import org.jetbrains.kotlin.daemon.common.ReplStateFacadeAsync
import org.jetbrains.kotlin.daemon.common.experimental.ReplStateFacadeClientSide
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

class RemoteReplCompilerStateHistoryAsync(private val state: RemoteReplCompilerStateAsync) : IReplStageHistory<Unit>,
    AbstractList<ReplHistoryRecord<Unit>>() {
    override val size: Int
        get() = runBlocking { state.replStateFacade.getHistorySize() }

    override fun get(index: Int): ReplHistoryRecord<Unit> = runBlocking {
        ReplHistoryRecord(state.replStateFacade.historyGet(index), Unit)
    }

    override fun push(id: ILineId, item: Unit) {
        throw NotImplementedError("push to remote history is not supported")
    }

    override fun pop(): ReplHistoryRecord<Unit>? {
        throw NotImplementedError("pop from remote history is not supported")
    }

    override fun reset(): Iterable<ILineId> =  runBlocking {
        state.replStateFacade.historyReset().apply {
            currentGeneration.incrementAndGet()
        }
    }

    override fun resetTo(id: ILineId): Iterable<ILineId> =  runBlocking {
        state.replStateFacade.historyResetTo(id).apply {
            currentGeneration.incrementAndGet()
        }
    }

    val currentGeneration = AtomicInteger(REPL_CODE_LINE_FIRST_GEN)

    override val lock: ReentrantReadWriteLock get() = state.lock
}

class RemoteReplCompilerStateAsync(
    internal val replStateFacade: ReplStateFacadeAsync,
    override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageState<Unit> {

    override val currentGeneration: Int get() = (history as RemoteReplCompilerStateHistory).currentGeneration.get()

    override val history: IReplStageHistory<Unit> =
        RemoteReplCompilerStateHistoryAsync(this)
}