/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.command

import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.JavaExecutionStack
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.ClassesByNameProvider
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.util.containers.ContainerUtil
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.isPreFlight
import org.jetbrains.kotlin.idea.debugger.safeLineNumber
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod


class CoroutineBuilder(val suspendContext: SuspendContextImpl) {
    private val coroutineStackFrameProvider = CoroutineAsyncStackTraceProvider()
    val debugProcess = suspendContext.debugProcess
    private val virtualMachineProxy = debugProcess.virtualMachineProxy

    companion object {
        const val CREATION_STACK_TRACE_SEPARATOR = "\b\b\b" // the "\b\b\b" is used as creation stacktrace separator in kotlinx.coroutines
    }

    fun build(coroutine: CoroutineInfoData): List<CoroutineStackFrameItem> {
        val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()

        if (coroutine.isRunning() && coroutine.activeThread is ThreadReference) {
            val threadReferenceProxyImpl = ThreadReferenceProxyImpl(debugProcess.virtualMachineProxy, coroutine.activeThread)

            val realFrames = threadReferenceProxyImpl.forceFrames()
            var coroutineStackInserted = false
            var preflightFound = false
            for (runningStackFrameProxy in realFrames) {
                if (runningStackFrameProxy.location().isPreFlight()) {
                    preflightFound = true
                    continue
                }
                if (preflightFound) {
                    val coroutineStack = coroutineStackFrameProvider.lookupForResumeContinuation(runningStackFrameProxy, suspendContext)
                    if (coroutineStack?.isNotEmpty() == true) {
                        // clue coroutine stack into the thread's real stack

                        for (asyncFrame in coroutineStack) {
                            coroutineStackFrameList.add(
                                RestoredCoroutineStackFrameItem(
                                    runningStackFrameProxy,
                                    asyncFrame.location,
                                    asyncFrame.spilledVariables
                                )
                            )
                            coroutineStackInserted = true
                        }
                    }
                    preflightFound = false
                }
                if (!(coroutineStackInserted && isInvokeSuspendNegativeLineMethodFrame(runningStackFrameProxy)))
                    coroutineStackFrameList.add(RunningCoroutineStackFrameItem(runningStackFrameProxy))
                coroutineStackInserted = false
            }
        } else if ((coroutine.isSuspended() || coroutine.activeThread == null) && coroutine.lastObservedFrameFieldRef is ObjectReference)
            coroutineStackFrameList.addAll(coroutine.stackTrace)

        coroutineStackFrameList.addAll(coroutine.creationStackTrace)
        coroutine.stackFrameList.addAll(coroutineStackFrameList)
        return coroutineStackFrameList
    }

    private fun isInvokeSuspendNegativeLineMethodFrame(frame: StackFrameProxyImpl) =
        frame.safeLocation()?.safeMethod()?.name() == "invokeSuspend" &&
                frame.safeLocation()?.safeMethod()?.signature() == "(Ljava/lang/Object;)Ljava/lang/Object;" &&
                frame.safeLocation()?.safeLineNumber() ?: 0 < 0
}