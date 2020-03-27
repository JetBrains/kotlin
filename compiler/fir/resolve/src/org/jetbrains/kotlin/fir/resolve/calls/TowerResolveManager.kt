/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import java.util.*
import kotlin.coroutines.*

class TowerResolveManager private constructor(private val isSuccess: () -> Boolean) {

    constructor(collector: CandidateCollector) : this({ collector.isSuccess() })

    private val queue = PriorityQueue<SuspendedResolverTask>()

    fun reset() {
        queue.clear()
    }

    private suspend fun suspendResolverTask(group: TowerGroup) = suspendCoroutine<Unit> { queue += SuspendedResolverTask(it, group) }

    suspend fun requestGroup(requested: TowerGroup) {
        val peeked = queue.peek()

        // Task ordering should be FIFO
        // Here, if peeked have equal group it means that it came first to this function, so should be processed before us
        if (peeked != null && peeked.group <= requested) {
            suspendResolverTask(requested)
        }
    }

    private suspend fun stopResolverTask(): Nothing = suspendCoroutine { }

    suspend fun stopIfSuccess() {
        if (isSuccess()) stopResolverTask()
    }

    private data class SuspendedResolverTask(
        val continuation: Continuation<Unit>,
        val group: TowerGroup
    ) : Comparable<SuspendedResolverTask> {
        override fun compareTo(other: SuspendedResolverTask): Int {
            return group.compareTo(other.group)
        }
    }

    fun enqueueResolverTask(group: TowerGroup = TowerGroup.Start, task: suspend () -> Unit) {
        val continuation = task.createCoroutine(
            object : Continuation<Unit> {
                override val context: CoroutineContext
                    get() = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    result.getOrThrow()
                }
            }
        )

        queue += SuspendedResolverTask(continuation, group)
    }

    fun runTasks() {
        while (queue.isNotEmpty()) {
            queue.poll().continuation.resume(Unit)
            if (isSuccess()) return
        }
    }
}

enum class InvokeResolveMode {
    IMPLICIT_CALL_ON_GIVEN_RECEIVER,
    RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
}
