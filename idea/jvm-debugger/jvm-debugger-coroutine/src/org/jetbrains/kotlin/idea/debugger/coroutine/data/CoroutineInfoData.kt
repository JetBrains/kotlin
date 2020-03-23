/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.LocationCache
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.isAbstractCoroutine
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

/**
 * Represents state of a coroutine.
 * @see `kotlinx.coroutines.debug.CoroutineInfo`
 */
data class CoroutineInfoData(
    val key: CoroutineNameIdState,
    val stackTrace: MutableList<CoroutineStackFrameItem>,
    val creationStackTrace: List<CreationCoroutineStackFrameItem>,
    val activeThread: ThreadReference? = null, // for suspended coroutines should be null
    val lastObservedFrameFieldRef: ObjectReference? = null
) {
    fun isSuspended() = key.state == State.SUSPENDED

    fun isCreated() = key.state == State.CREATED

    fun isEmptyStack() = stackTrace.isEmpty()

    fun isRunning() = key.state == State.RUNNING

    companion object {
        val log by logger
        const val DEFAULT_COROUTINE_NAME = "coroutine"
        const val DEFAULT_COROUTINE_STATE = "UNKNOWN"

        fun lookup(
            input: ObjectReference?,
            context: DefaultExecutionContext,
            stackFrameItems: List<CoroutineStackFrameItem>
        ): CoroutineInfoData? {
            val locationCache = LocationCache(context)
            val creationStackTrace = mutableListOf<CreationCoroutineStackFrameItem>()
            val realState = if (input?.type()?.isAbstractCoroutine() ?: false) {
                state(input, context) ?: return null
            } else {
                val ci = DebugProbesImpl(context).getCoroutineInfo(input, context)
                if (ci != null) {
                    if (ci.creationStackTrace != null)
                        for (frame in ci.creationStackTrace.mapNotNull { it.stackTraceElement() }) {
                            creationStackTrace.add(CreationCoroutineStackFrameItem(frame, locationCache.createLocation(frame)))
                        }
                    CoroutineNameIdState.instance(ci)
                } else {
                    log.warn("Coroutine information not found, ${input?.type()} is not subtype of AbstractCoroutine as expected.")
                    CoroutineNameIdState(DEFAULT_COROUTINE_NAME, "-1", State.UNKNOWN, null)
                }
            }
            return CoroutineInfoData(realState, stackFrameItems.toMutableList(), creationStackTrace)
        }

        fun state(value: ObjectReference?, context: DefaultExecutionContext): CoroutineNameIdState? {
            value ?: return null
            val reference = JavaLangMirror(context)
            val standaloneCoroutine = StandaloneCoroutine(context)
            val standAloneCoroutineMirror = standaloneCoroutine.mirror(value, context)
            if (standAloneCoroutineMirror?.context is MirrorOfCoroutineContext) {
                val id = standAloneCoroutineMirror.context.id
                val name = standAloneCoroutineMirror.context.name ?: DEFAULT_COROUTINE_NAME
                val toString = reference.string(value, context)
                val r = """\w+\{(\w+)\}\@([\w\d]+)""".toRegex()
                val matcher = r.toPattern().matcher(toString)
                if (matcher.matches()) {
                    val state = stateOf(matcher.group(1))
                    val hexAddress = matcher.group(2)
                    return CoroutineNameIdState(name, id?.toString() ?: hexAddress, state, standAloneCoroutineMirror.context.dispatcher)
                }
            }
            return null
        }

        private fun stateOf(state: String?): State =
            when (state) {
                "Active" -> State.RUNNING
                "Cancelling" -> State.SUSPENDED_CANCELLING
                "Completing" -> State.SUSPENDED_COMPLETING
                "Cancelled" -> State.CANCELLED
                "Completed" -> State.COMPLETED
                "New" -> State.NEW
                else -> State.UNKNOWN
            }
    }
}

data class CoroutineNameIdState(val name: String, val id: String, val state: State, val dispatcher: String?) {
    companion object {
        fun instance(mirror: MirrorOfCoroutineInfo): CoroutineNameIdState =
            CoroutineNameIdState(
                mirror.context?.name ?: "coroutine",
                "${mirror.sequenceNumber}",
                State.valueOf(mirror.state ?: "UNKNOWN"),
                mirror.context?.dispatcher
            )
    }
}

enum class State {
    RUNNING,
    SUSPENDED,
    CREATED,
    UNKNOWN,
    SUSPENDED_COMPLETING,
    SUSPENDED_CANCELLING,
    CANCELLED,
    COMPLETED,
    NEW
}