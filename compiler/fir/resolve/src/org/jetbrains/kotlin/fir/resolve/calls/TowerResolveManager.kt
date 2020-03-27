/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import java.util.*
import kotlin.coroutines.*

class TowerResolveManager internal constructor() {

    private val queue = PriorityQueue<SuspendedResolverTask>()

    // TODO: Get rid of the property (it should be passed in a different way)
    internal lateinit var callResolutionContext: CallResolutionContext

    private fun isSuccess() = callResolutionContext.resultCollector.isSuccess()

    fun reset() {
        queue.clear()
    }

    private suspend fun suspendResolverTask(group: TowerGroup) = suspendCoroutine<Unit> { queue += SuspendedResolverTask(it, group) }

    private suspend fun requestGroup(requested: TowerGroup) {
        val peeked = queue.peek()

        // Task ordering should be FIFO
        // Here, if peeked have equal group it means that it came first to this function, so should be processed before us
        if (peeked != null && peeked.group <= requested) {
            suspendResolverTask(requested)
        }
    }

    private suspend fun stopResolverTask(): Nothing = suspendCoroutine { }

    suspend fun processLevel(
        towerLevel: SessionBasedTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        invokeResolveMode: InvokeResolveMode? = null
    ): ProcessorAction {
        requestGroup(group)

        val result = with(LevelHandler(callInfo, explicitReceiverKind, group, callResolutionContext, this)) {
            towerLevel.handleLevel(invokeResolveMode)
        }

        if (isSuccess()) stopResolverTask()

        return result
    }

    suspend fun processLevelForPropertyWithInvoke(
        towerLevel: SessionBasedTowerLevel,
        callInfo: CallInfo,
        group: TowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
    ) {
        if (callInfo.callKind == CallKind.Function) {
            processLevel(towerLevel, callInfo, group, explicitReceiverKind, InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION)
        }
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
