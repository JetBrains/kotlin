/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.command

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.*
import com.intellij.debugger.jdi.ClassesByNameProvider
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.openapi.project.Project
import com.intellij.util.containers.ContainerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.data.SuspendStackFrameDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutines.data.SyntheticStackFrame
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.AsyncStackTraceContext
import org.jetbrains.kotlin.idea.debugger.coroutines.util.EmptyStackFrameDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext


class CoroutineBuilder(val suspendContext: XSuspendContext) {
    private val methodsTracker = MethodsTracker()
    private val coroutineStackFrameProvider = CoroutineAsyncStackTraceProvider()
    val debugProcess = (suspendContext as SuspendContextImpl).debugProcess
    val virtualMachineProxy = debugProcess.virtualMachineProxy
    val classesByName = ClassesByNameProvider.createCache(virtualMachineProxy.allClasses())

    companion object {
        val CREATION_STACK_TRACE_SEPARATOR = "\b\b\b" // the "\b\b\b" is used for creation stacktrace separator in kotlinx.coroutines
    }

    fun build(coroutine: CoroutineInfoData): List<CoroutineStackFrameItem> {
        val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()
        val firstSuspendedStackFrameProxyImpl = firstSuspendedThreadFrame()
        val creationFrameSeparatorIndex = findCreationFrameIndex(coroutine.stackTrace)
        val positionManager = debugProcess.positionManager

        if (coroutine.state == CoroutineInfoData.State.RUNNING && coroutine.activeThread is ThreadReference) {
            val threadReferenceProxyImpl = runningThreadProxy(coroutine.activeThread)
            val executionStack = JavaExecutionStack(threadReferenceProxyImpl, debugProcess, suspendedSameThread(coroutine.activeThread))

            val frames = threadReferenceProxyImpl.forceFrames()
            var resumeMethodIndex = findResumeMethodIndex(frames)
            for (frameIndex in 0..frames.lastIndex) {
                val runningStackFrameProxy = frames[frameIndex]
                if (frameIndex == resumeMethodIndex) {
                    val previousFrame = frames[resumeMethodIndex - 1]
                    val previousJavaFrame = JavaStackFrame(StackFrameDescriptorImpl(previousFrame, methodsTracker), true)
                    val asyncStackTrace = coroutineStackFrameProvider
                        .getAsyncStackTrace(previousJavaFrame, suspendContext)
                    asyncStackTrace?.forEach { asyncFrame ->
                        val xStackFrame = JavaStackFrame(StackFrameDescriptorImpl(previousFrame, methodsTracker), true)
                        coroutineStackFrameList.add(AsyncCoroutineStackFrameItem(runningStackFrameProxy, asyncFrame, xStackFrame))
                    }
                } else {
                    val xStackFrame = executionStack.createStackFrame(runningStackFrameProxy)
                    coroutineStackFrameList.add(RunningCoroutineStackFrameItem(runningStackFrameProxy, xStackFrame))
                }
            }
        } else if (coroutine.state == CoroutineInfoData.State.SUSPENDED || coroutine.activeThread == null) {
            // to get frames from CoroutineInfo anyway
            // the thread is paused on breakpoint - it has at least one frame
            val suspendedStackTrace = coroutine.stackTrace.take(creationFrameSeparatorIndex + 1)
            for (suspendedFrame in suspendedStackTrace) {
                val suspendedXStackFrame = stackFrame(positionManager, firstSuspendedStackFrameProxyImpl, suspendedFrame)
                coroutineStackFrameList.add(
                    SuspendCoroutineStackFrameItem(firstSuspendedStackFrameProxyImpl, suspendedFrame, suspendedXStackFrame)
                )
            }
        }

        val executionStack = JavaExecutionStack(suspendedThreadProxy(), debugProcess, false)
        val xStackFrame = executionStack.createStackFrame(firstSuspendedStackFrameProxyImpl)

        coroutine.stackTrace.subList(creationFrameSeparatorIndex + 1, coroutine.stackTrace.size).forEach {
            var location = createLocation(it)
            coroutineStackFrameList.add(CreationCoroutineStackFrameItem(firstSuspendedStackFrameProxyImpl, it, xStackFrame, location))
        }
        coroutine.stackFrameList.addAll(coroutineStackFrameList)
        return coroutineStackFrameList
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

    fun stackFrame(positionManager: CompoundPositionManager, runningStackFrameProxy: StackFrameProxyImpl, location: Location): XStackFrame {
        return positionManager.createStackFrame(runningStackFrameProxy, debugProcess, location)!!
    }

    fun stackFrame(positionManager: CompoundPositionManager, runningStackFrameProxy: StackFrameProxyImpl, stackTraceElement: StackTraceElement) : XStackFrame {
        val location = createLocation(stackTraceElement)
        return positionManager.createStackFrame(runningStackFrameProxy, debugProcess, location)!!
    }

    /**
     * Tries to find creation frame separator if any, returns last index if none found
     */
    private fun findCreationFrameIndex(frames: List<StackTraceElement>): Int {
        var index = frames.indexOfFirst { isCreationSeparatorFrame(it) }
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

    private fun findResumeMethodIndex(frames: List<StackFrameProxyImpl>): Int {
        for (i: Int in frames.lastIndex downTo 0)
            if (isResumeMethodFrame(frames[i])) {
                return i
            }
        return 0
    }

    private fun isResumeMethodFrame(frame: StackFrameProxyImpl) =
        frame.safeLocation()?.safeMethod()?.name() == "resumeWith"

    /**
     * Should be invoked on manager thread
     * This code was migrated from previous implementation, has to be refactored @TODO
     */
    private fun createSyntheticStackFrame(
        descriptor: SuspendStackFrameDescriptor,
        pos: XSourcePosition,
        project: Project
    ): Pair<XExecutionStack, SyntheticStackFrame>? {
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val suspendContext = context.suspendContext ?: return null
        val proxy = suspendContext.thread ?: return null
        val executionStack = JavaExecutionStack(proxy, suspendContext.debugProcess, false)
        executionStack.initTopFrame()
        val evalContext = context.createEvaluationContext()
        val frameProxy = evalContext?.frameProxy ?: return null
        val execContext = ExecutionContext(evalContext, frameProxy)
        val continuation = descriptor.continuation // guaranteed that it is a BaseContinuationImpl
        val aMethod = (continuation.type() as ClassType).concreteMethodByName(
            "getStackTraceElement",
            "()Ljava/lang/StackTraceElement;"
        )
        val vars = with(CoroutineAsyncStackTraceProvider()) {
            AsyncStackTraceContext(
                execContext,
                aMethod
            ).getSpilledVariables(continuation)
        } ?: return null
        return executionStack to SyntheticStackFrame(descriptor, vars, pos)
    }
}

class CreationCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    val stackTraceElement: StackTraceElement,
    stackFrame: XStackFrame,
    val location: Location
) : CoroutineStackFrameItem(frame, stackFrame) {
    override fun location() = location
    fun emptyDescriptor() =
        EmptyStackFrameDescriptor(stackTraceElement, frame)
}

class SuspendCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    val stackTraceElement: StackTraceElement,
    stackFrame: XStackFrame
) : CoroutineStackFrameItem(frame, stackFrame) {
    override fun location() = frame.location()
}

class AsyncCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    val frameItem: StackFrameItem,
    stackFrame: XStackFrame
) : CoroutineStackFrameItem(frame, stackFrame) {
    override fun location() : Location = frame.location()
}

class RunningCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    stackFrame: XStackFrame
) : CoroutineStackFrameItem(frame, stackFrame) {
    val location = frame.location() // it should be invoked in manager thread

    override fun location() = location
}

abstract class CoroutineStackFrameItem(val frame: StackFrameProxyImpl, val stackFrame: XStackFrame) {
    val log by logger

    fun sourcePosition() : XSourcePosition? = stackFrame.sourcePosition

    abstract fun location(): Location

    fun uniqueId(): String {
        val location = location()
        try {
            return location.safeSourceName() + ":" + location.safeMethod().toString() + ":" +
                    location.safeLineNumber() + ":" + location.safeSourceLineNumber()
        } catch (e: Exception) {
            log.error(e)
            return location.method().toString() + ":" + location.lineNumber()
        }
    }
}