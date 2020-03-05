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
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.safeLineNumber
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod


class CoroutineBuilder(val suspendContext: XSuspendContext) {
    private val coroutineStackFrameProvider = CoroutineAsyncStackTraceProvider()
    val debugProcess = (suspendContext as SuspendContextImpl).debugProcess
    private val virtualMachineProxy = debugProcess.virtualMachineProxy
    private val classesByName = ClassesByNameProvider.createCache(virtualMachineProxy.allClasses())

    companion object {
        const val CREATION_STACK_TRACE_SEPARATOR = "\b\b\b" // the "\b\b\b" is used for creation stacktrace separator in kotlinx.coroutines
    }

    fun build(coroutine: CoroutineInfoData): List<CoroutineStackFrameItem> {
        val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()
        val firstSuspendedStackFrameProxyImpl = firstSuspendedThreadFrame()
        val creationFrameSeparatorIndex = findCreationFrameIndex(coroutine.stackTrace)

        if (coroutine.state == CoroutineInfoData.State.RUNNING && coroutine.activeThread is ThreadReference) {
            val threadReferenceProxyImpl = runningThreadProxy(coroutine.activeThread)
            val executionStack = JavaExecutionStack(threadReferenceProxyImpl, debugProcess, suspendedSameThread(coroutine.activeThread))

            val frames = threadReferenceProxyImpl.forceFrames()
            var coroutineStackInserted = false
            for (runningStackFrameProxy in frames) {
                val jStackFrame = executionStack.createStackFrame(runningStackFrameProxy)
                val coroutineStack = coroutineStackFrameProvider.getAsyncStackTraceSafe(runningStackFrameProxy, suspendContext)
                if (coroutineStack.isNotEmpty()) {
                    // clue coroutine stack into the thread's real stack

                    val firstMergedFrame = mergeFrameVars(coroutineStack.first(), runningStackFrameProxy, jStackFrame)
                    coroutineStackFrameList.add(firstMergedFrame)

                    for (asyncFrame in coroutineStack.drop(1)) {
                        coroutineStackFrameList.add(
                            RestoredCoroutineStackFrameItem(
                                runningStackFrameProxy,
                                asyncFrame.location,
                                asyncFrame.spilledVariables
                            )
                        )
                        coroutineStackInserted = true
                    }
                } else {
                    if (coroutineStackInserted && isInvokeSuspendNegativeLineMethodFrame(runningStackFrameProxy)) {
                        coroutineStackInserted = false
                    } else
                        coroutineStackFrameList.add(RunningCoroutineStackFrameItem(runningStackFrameProxy, jStackFrame))
                }
            }
        } else if ((coroutine.state == CoroutineInfoData.State
                .SUSPENDED || coroutine.activeThread == null) && coroutine.lastObservedFrameFieldRef is ObjectReference
        ) {
            // to get frames from CoroutineInfo anyway
            // the thread is paused on breakpoint - it has at least one frame
            val suspendedStackTrace = coroutine.stackTrace.take(creationFrameSeparatorIndex)
            for (suspendedFrame in suspendedStackTrace) {
                val location = createLocation(suspendedFrame)
                coroutineStackFrameList.add(
                    SuspendCoroutineStackFrameItem(
                        firstSuspendedStackFrameProxyImpl,
                        suspendedFrame,
                        coroutine.lastObservedFrameFieldRef,
                        location
                    )
                )
            }
        }

        coroutine.stackTrace.subList(creationFrameSeparatorIndex + 1, coroutine.stackTrace.size).forEach {
            val location = createLocation(it)
            coroutineStackFrameList.add(CreationCoroutineStackFrameItem(firstSuspendedStackFrameProxyImpl, it, location))
        }
        coroutine.stackFrameList.addAll(coroutineStackFrameList)
        return coroutineStackFrameList
    }

    /**
     * First frames need to be merged as real frame has accurate line number but lacks local variables from coroutine-restored frame.
     */
    private fun mergeFrameVars(
        restoredFrame: CoroutineStackFrameItem,
        runningStackFrameProxy: StackFrameProxyImpl,
        jStackFrame: XStackFrame,
    ): RunningCoroutineStackFrameItem {
        if (restoredFrame.location is GeneratedLocation) {
            val restoredMethod = restoredFrame.location.method()
            val realMethod = runningStackFrameProxy.location().method()
            // if refers to the same method - proceed with merge, otherwise do nothing
            if (restoredMethod == realMethod) {
                return RunningCoroutineStackFrameItem(runningStackFrameProxy, jStackFrame, restoredFrame.spilledVariables)
            }
        }
        return RunningCoroutineStackFrameItem(runningStackFrameProxy, jStackFrame)
    }

    private fun suspendedSameThread(activeThread: ThreadReference) =
        activeThread == suspendedThreadProxy().threadReference

    private fun createLocation(stackTraceElement: StackTraceElement): Location = findLocation(
        ContainerUtil.getFirstItem(classesByName[stackTraceElement.className]),
        stackTraceElement.methodName,
        stackTraceElement.lineNumber
    )

    private fun findLocation(
        type: ReferenceType?,
        methodName: String,
        line: Int
    ): Location {
        if (type != null && line >= 0) {
            try {
                val location = type.locationsOfLine(DebugProcess.JAVA_STRATUM, null, line).stream()
                    .filter { l: Location -> l.method().name() == methodName }
                    .findFirst().orElse(null)
                if (location != null) {
                    return location
                }
            } catch (ignored: AbsentInformationException) {
            }
        }
        return GeneratedLocation(debugProcess, type, methodName, line)
    }

    /**
     * Tries to find creation frame separator if any, returns last index if none found
     */
    private fun findCreationFrameIndex(frames: List<StackTraceElement>): Int {
        val index = frames.indexOfFirst { isCreationSeparatorFrame(it) }
        return if (index < 0)
            frames.lastIndex
        else
            index
    }

    private fun isCreationSeparatorFrame(it: StackTraceElement) =
        it.className.startsWith(CREATION_STACK_TRACE_SEPARATOR)

    private fun firstSuspendedThreadFrame(): StackFrameProxyImpl =
        suspendedThreadProxy().forceFrames().first()

    // retrieves currently suspended but active and executing coroutine thread proxy
    fun runningThreadProxy(threadReference: ThreadReference) =
        ThreadReferenceProxyImpl(debugProcess.virtualMachineProxy, threadReference)

    // retrieves current suspended thread proxy
    private fun suspendedThreadProxy(): ThreadReferenceProxyImpl =
        (suspendContext as SuspendContextImpl).thread!! // @TODO hash replace !!

    private fun isInvokeSuspendNegativeLineMethodFrame(frame: StackFrameProxyImpl) =
        frame.safeLocation()?.safeMethod()?.name() == "invokeSuspend" &&
                frame.safeLocation()?.safeMethod()?.signature() == "(Ljava/lang/Object;)Ljava/lang/Object;" &&
                frame.safeLocation()?.safeLineNumber() ?: 0 < 0
}