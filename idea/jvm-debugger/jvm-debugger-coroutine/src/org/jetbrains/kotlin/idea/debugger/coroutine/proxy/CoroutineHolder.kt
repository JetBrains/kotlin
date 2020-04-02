/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineNameIdState
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.data.State
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.JavaLangMirror
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.MirrorOfCoroutineContext
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

data class CoroutineHolder(
    val value: ObjectReference?,
    val info: CoroutineNameIdState,
    val stackFrameItems: List<CoroutineStackFrameItem>
) {
    companion object {
        fun lookup(
            value: ObjectReference?,
            context: DefaultExecutionContext,
            stackFrameItems: List<CoroutineStackFrameItem>
        ): CoroutineHolder? {
            val state = state(value, context) ?: return null
            val realState =
                if (stackFrameItems.isEmpty()) state.copy(state = State.CREATED) else state.copy(state = State.SUSPENDED)
            return CoroutineHolder(value, realState, stackFrameItems)
        }

        fun state(value: ObjectReference?, context: DefaultExecutionContext): CoroutineNameIdState? {
            value ?: return null
            val reference = JavaLangMirror(context)
            val standAloneCoroutineMirror = reference.standaloneCoroutine.mirror(value, context)
            if (standAloneCoroutineMirror?.context is MirrorOfCoroutineContext) {
                val id = standAloneCoroutineMirror.context.id
                val toString = reference.string(value, context)
                val r = """\w+\{(\w+)\}\@([\w\d]+)""".toRegex()
                val matcher = r.toPattern().matcher(toString)
                if (matcher.matches()) {
                    val state = stateOf(matcher.group(1))
                    val hexAddress = matcher.group(2)
                    return CoroutineNameIdState(standAloneCoroutineMirror.context.name, id?.toString() ?: hexAddress, state)
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


