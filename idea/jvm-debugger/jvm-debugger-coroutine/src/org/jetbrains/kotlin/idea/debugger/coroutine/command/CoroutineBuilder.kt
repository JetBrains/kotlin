/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.command

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ContinuationHolder.Companion.leftThreadStack
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.CoroutinePreflightStackFrame
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.isPreFlight
import org.jetbrains.kotlin.idea.debugger.safeLineNumber
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod


class CoroutineBuilder(val suspendContext: SuspendContextImpl) {
    private val coroutineStackFrameProvider = CoroutineAsyncStackTraceProvider()
    val debugProcess = suspendContext.debugProcess

    companion object {
        const val CREATION_STACK_TRACE_SEPARATOR = "\b\b\b" // the "\b\b\b" is used as creation stacktrace separator in kotlinx.coroutines
    }

    fun build(coroutine: CoroutineInfoData): List<CoroutineStackFrameItem> {
        val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()

        if (coroutine.isRunning() && coroutine.activeThread is ThreadReference) {
            val threadReferenceProxyImpl = ThreadReferenceProxyImpl(debugProcess.virtualMachineProxy, coroutine.activeThread)

            val realFrames = threadReferenceProxyImpl.forceFrames()
            for (runningStackFrameProxy in realFrames) {
                if (runningStackFrameProxy.location().isPreFlight()) {
                    val leftThreadStack = leftThreadStack(runningStackFrameProxy) ?: continue
                    val coroutineStack =
                        coroutineStackFrameProvider.lookupForResumeContinuation(runningStackFrameProxy, suspendContext, leftThreadStack) ?: continue
                    coroutineStackFrameList.add(RunningCoroutineStackFrameItem(coroutineStack.stackFrameProxy))
                    // clue coroutine stack into the thread's real stack
                    val stackFrameItems = coroutineStack.coroutineInfoData.stackTrace.map {
                        RestoredCoroutineStackFrameItem(
                            runningStackFrameProxy,
                            it.location,
                            it.spilledVariables
                        )
                    }
                    coroutineStackFrameList.addAll(stackFrameItems)
                } else
                    coroutineStackFrameList.add(RunningCoroutineStackFrameItem(runningStackFrameProxy))
            }
        } else if (coroutine.isSuspended())
            coroutineStackFrameList.addAll(coroutine.stackTrace)

        coroutineStackFrameList.addAll(coroutine.creationStackTrace)
        return coroutineStackFrameList
    }
}