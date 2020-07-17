/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData.Companion.DEFAULT_COROUTINE_NAME
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData.Companion.DEFAULT_COROUTINE_STATE
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.MirrorOfCoroutineInfo
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger

/**
 * Represents state of a coroutine.
 * @see `kotlinx.coroutines.debug.CoroutineInfo`
 */
data class CoroutineInfoData(
    val key: CoroutineNameIdState,
    val stackTrace: List<CoroutineStackFrameItem>,
    val creationStackTrace: List<CreationCoroutineStackFrameItem>,
    val activeThread: ThreadReference? = null, // for suspended coroutines should be null
    val lastObservedFrame: ObjectReference? = null
) {
    fun isSuspended() = key.state == State.SUSPENDED

    fun isCreated() = key.state == State.CREATED

    fun isEmptyStack() = stackTrace.isEmpty()

    fun isRunning() = key.state == State.RUNNING

    private fun topRestoredFrame() = stackTrace.firstOrNull()

    fun topFrameVariables() = topRestoredFrame()?.spilledVariables ?: emptyList()

    companion object {
        val log by logger
        const val DEFAULT_COROUTINE_NAME = "coroutine"
        const val DEFAULT_COROUTINE_STATE = "UNKNOWN"
    }
}

data class CoroutineNameIdState(val name: String, val id: String, val state: State, val dispatcher: String?) {

    fun formatName() =
        "$name:$id"
    
    companion object {
        fun instance(mirror: MirrorOfCoroutineInfo): CoroutineNameIdState =
            CoroutineNameIdState(
                mirror.context?.name ?: DEFAULT_COROUTINE_NAME,
                "${mirror.sequenceNumber}",
                State.valueOf(mirror.state ?: DEFAULT_COROUTINE_STATE),
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