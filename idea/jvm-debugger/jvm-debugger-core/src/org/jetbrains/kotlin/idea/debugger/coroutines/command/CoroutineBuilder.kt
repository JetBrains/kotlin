/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.command

import com.intellij.debugger.engine.*
import com.intellij.debugger.jdi.ClassesByNameProvider
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.util.containers.ContainerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.util.getPosition


class CoroutineBuilder(val suspendContext: SuspendContextImpl) {
    private val methodsTracker = MethodsTracker()
    private val coroutineStackFrameProvider = CoroutineAsyncStackTraceProvider()
    val virtualMachineProxy = suspendContext.debugProcess.virtualMachineProxy
    val classesByName = ClassesByNameProvider.createCache(virtualMachineProxy.allClasses())

    companion object {
        val CREATION_STACK_TRACE_SEPARATOR = "\b\b\b" // the "\b\b\b" is used for creation stacktrace separator in kotlinx.coroutines
    }

    fun build(ci: CoroutineInfoData): List<CoroutineStackFrameItem> {
        val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()
        val suspendedThreadProxy = suspendedThreadProxy()
        val firstSuspendedStackFrameProxyImpl = firstSuspendedThreadFrame()
        val creationFrameSeparatorIndex = findCreationFrameIndex(ci.stackTrace)
        val runningThreadReferenceProxyImpl = currentRunningThreadProxy(ci.thread ?: suspendedThreadProxy().threadReference)
        val positionManager = suspendContext.debugProcess.positionManager

        if (ci.state == CoroutineInfoData.State.RUNNING && runningThreadReferenceProxyImpl is ThreadReferenceProxyImpl) {
            val executionStack = JavaExecutionStack(runningThreadReferenceProxyImpl, suspendContext.debugProcess, suspendContext.thread == runningThreadReferenceProxyImpl)

            val frames = runningThreadReferenceProxyImpl.forceFrames()
            var resumeMethodIndex = findResumeMethodIndex(frames)
            for (frameIndex in 0..frames.lastIndex) {
                val runningStackFrameProxy = frames[frameIndex]
                if (frameIndex == resumeMethodIndex) {
                    val previousFrame = frames[resumeMethodIndex - 1]
                    val previousJavaFrame = JavaStackFrame(StackFrameDescriptorImpl(previousFrame, methodsTracker), true)
                    val asyncStackTrace = coroutineStackFrameProvider
                        .getAsyncStackTrace(previousJavaFrame, suspendContext)
                    asyncStackTrace?.forEach {
//                        val xStackFrame = positionManager.createStackFrame(runningStackFrameProxy, suspendContext.debugProcess, it.location)
                        val xStackFrame = JavaStackFrame(StackFrameDescriptorImpl(previousFrame, methodsTracker), true)
//                        val itStackFrame = null
                        coroutineStackFrameList.add(AsyncCoroutineStackFrameItem(runningStackFrameProxy, "some label", it, xStackFrame))
                    }
                } else {
//                    val xStackFrame = stackFrame(positionManager, runningStackFrameProxy, runningStackFrameProxy.location())
                    val xStackFrame = executionStack.createStackFrame(runningStackFrameProxy)
                    coroutineStackFrameList.add(RunningCoroutineStackFrameItem(runningStackFrameProxy, "", xStackFrame))
                }
            }
        } else if (ci.state == CoroutineInfoData.State
                .SUSPENDED || runningThreadReferenceProxyImpl == null
        ) { // to get frames from CoroutineInfo anyway
            // the thread is paused on breakpoint - it has at least one frame
            ci.stackTrace.subList(0, creationFrameSeparatorIndex).forEach {
                val xStackFrame = stackFrame(positionManager, firstSuspendedStackFrameProxyImpl, it)
//                val xStackFrame = null
                coroutineStackFrameList.add(SuspendCoroutineStackFrameItem(firstSuspendedStackFrameProxyImpl, "suspended frame", it, xStackFrame))
            }
        }

        val executionStack = JavaExecutionStack(suspendedThreadProxy(), suspendContext.debugProcess, false)
        val xStackFrame = executionStack.createStackFrame(firstSuspendedStackFrameProxyImpl)

        ci.stackTrace.subList(creationFrameSeparatorIndex + 1, ci.stackTrace.size).forEach {
            var location = createLocation(it)
            coroutineStackFrameList.add(CreationCoroutineStackFrameItem(firstSuspendedStackFrameProxyImpl, "creation frame", it, xStackFrame, location))
        }
        ci.stackFrameList.addAll(coroutineStackFrameList)
        return coroutineStackFrameList
    }

    fun createLocation(stackTraceElement: StackTraceElement): Location {
       return findLocation(
            ContainerUtil.getFirstItem(classesByName[stackTraceElement.className]),
            stackTraceElement.methodName,
            stackTraceElement.lineNumber
        )
    }

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
        return GeneratedLocation(suspendContext.debugProcess, type, methodName, line)
    }

    fun stackFrame(positionManager: CompoundPositionManager, runningStackFrameProxy: StackFrameProxyImpl, location: Location): XStackFrame {
        return positionManager.createStackFrame(runningStackFrameProxy, suspendContext.debugProcess, location)!!
    }

    fun stackFrame(positionManager: CompoundPositionManager, runningStackFrameProxy: StackFrameProxyImpl, stackTraceElement: StackTraceElement) : XStackFrame {
        val location = createLocation(stackTraceElement)
//        val sourcePosition = getPosition(stackTraceElement)
        return positionManager.createStackFrame(runningStackFrameProxy, suspendContext.debugProcess, location)!!
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

    // retrieves currently suspended but active and executing corouting thread proxy
    fun currentRunningThreadProxy(threadReference: ThreadReference?): ThreadReferenceProxyImpl? =
        ThreadReferenceProxyImpl(suspendContext.debugProcess.virtualMachineProxy, threadReference)

    // retrieves current suspended thread proxy
    fun suspendedThreadProxy(): ThreadReferenceProxyImpl =
        suspendContext.thread!! // @TODO hash replace !!

    private fun findResumeMethodIndex(frames: List<StackFrameProxyImpl>): Int {
        for (j: Int in frames.lastIndex downTo 0)
            if (isResumeMethodFrame(frames[j])) {
                return j
            }
        return 0
    }

    private fun isResumeMethodFrame(frame: StackFrameProxyImpl) = frame.location().method().name() == "resumeWith"

    /**
     * Should be invoked on manager thread
     */
/*
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

 */
}

class CreationCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    label: String = "",
    val stackTraceElement: StackTraceElement,
    stackFrame: XStackFrame,
    val location: Location
) : CoroutineStackFrameItem(frame, label, stackFrame) {
    override fun location() = location
}

class SuspendCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    label: String = "",
    val stackTraceElement: StackTraceElement,
    stackFrame: XStackFrame
) : CoroutineStackFrameItem(frame, label, stackFrame) {
    override fun location() = frame.location()
}

class AsyncCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    label: String = "",
    val frameItem: StackFrameItem,
    stackFrame: XStackFrame
) : CoroutineStackFrameItem(frame, label, stackFrame) {
    override fun location() : Location = frame.location()
}

class RunningCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    label: String = "",
    stackFrame: XStackFrame
) : CoroutineStackFrameItem(frame, label, stackFrame) {
    val location = frame.location() // it should be invoked in manager thread

    override fun location() = location
}

abstract class CoroutineStackFrameItem(val frame: StackFrameProxyImpl, val label: String = "", val stackFrame: XStackFrame) {
    fun sourcePosition() : XSourcePosition? = stackFrame.sourcePosition

    abstract fun location(): Location
}