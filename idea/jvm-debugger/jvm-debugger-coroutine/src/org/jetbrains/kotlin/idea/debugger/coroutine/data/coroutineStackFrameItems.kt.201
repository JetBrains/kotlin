/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.impl.frame.XDebuggerFramesList
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutine.KotlinDebuggerCoroutinesBundle
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.LocationStackFrameProxyImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.util.findPosition
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.stackFrame.KotlinStackFrame

/**
 * Creation frame of coroutine either in RUNNING or SUSPENDED state.
 */
class CreationCoroutineStackFrameItem(
    val stackTraceElement: StackTraceElement,
    location: Location,
    val first: Boolean
) : CoroutineStackFrameItem(location, emptyList()) {

    override fun createFrame(debugProcess: DebugProcessImpl): XStackFrame? {
        return debugProcess.invokeInManagerThread {
            val frame = debugProcess.findFirstFrame() ?: return@invokeInManagerThread null
            val locationFrame = LocationStackFrameProxyImpl(location, frame)
            val position = location.findPosition(debugProcess.project)
            CreationCoroutineStackFrame(locationFrame, position, first)
        }
    }
}

/**
 * Restored frame in SUSPENDED coroutine, not attached to any thread.
 */
class SuspendCoroutineStackFrameItem(
    val stackTraceElement: StackTraceElement,
    location: Location,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(location, spilledVariables)


/**
 * Restored from memory dump
 */
class DefaultCoroutineStackFrameItem(location: Location, spilledVariables: List<XNamedValue>) :
    CoroutineStackFrameItem(location, spilledVariables) {

    override fun createFrame(debugProcess: DebugProcessImpl): XStackFrame? {
        return debugProcess.invokeInManagerThread {
            val frame = debugProcess.findFirstFrame() ?: return@invokeInManagerThread null
            val locationStackFrameProxyImpl = LocationStackFrameProxyImpl(location, frame)
            val position = location.findPosition(debugProcess.project) ?: return@invokeInManagerThread null
            CoroutineStackFrame(locationStackFrameProxyImpl, position, spilledVariables, false)
        }
    }
}

/**
 * Original frame appeared before resumeWith call.
 *
 * Sequence is the following
 *
 * - KotlinStackFrame
 * - invokeSuspend(KotlinStackFrame) -|
 *                                    | replaced with CoroutinePreflightStackFrame
 * - resumeWith(KotlinStackFrame) ----|
 * - Kotlin/JavaStackFrame -> PreCoroutineStackFrameItem : CoroutinePreflightStackFrame.threadPreCoroutineFrames
 *
 */
open class RunningCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(frame.location(), spilledVariables) {
    override fun createFrame(debugProcess: DebugProcessImpl): XStackFrame? {
        return debugProcess.invokeInManagerThread {
            val position = frame.location().findPosition(debugProcess.project)
            CoroutineStackFrame(frame, position)
        }
    }
}

sealed class CoroutineStackFrameItem(val location: Location, val spilledVariables: List<XNamedValue>) :
    StackFrameItem(location, spilledVariables) {
    val log by logger

    override fun createFrame(debugProcess: DebugProcessImpl): XStackFrame? {
        return debugProcess.invokeInManagerThread {
            val frame = debugProcess.findFirstFrame() ?: return@invokeInManagerThread null
            val locationFrame = LocationStackFrameProxyImpl(location, frame)
            val position = location.findPosition(debugProcess.project)
            CoroutineStackFrame(locationFrame, position)
        }
    }

    fun uniqueId() =
        location.safeSourceName() + ":" + location.safeMethod().toString() + ":" +
                location.safeLineNumber() + ":" + location.safeKotlinPreferredLineNumber()
}

fun DebugProcessImpl.findFirstFrame(): StackFrameProxyImpl? =
    suspendManager.pausedContext.thread?.forceFrames()?.firstOrNull()

/**
 * Coroutine exit frame represented by a stack frames
 * invokeSuspend():-1
 * resumeWith()
 *
 */
class CoroutinePreflightFrame(
    val coroutineInfoData: CoroutineInfoData,
    val frame: StackFrameProxyImpl,
    val threadPreCoroutineFrames: List<StackFrameProxyImpl>,
    val mode: SuspendExitMode,
    private val firstFrameVariables: List<XNamedValue> = coroutineInfoData.topFrameVariables()
) : CoroutineStackFrame(frame, null, firstFrameVariables) {

    override fun isInLibraryContent() = false

    override fun isSynthetic() = false

}

class CreationCoroutineStackFrame(
    frame: StackFrameProxyImpl,
    sourcePosition: XSourcePosition?,
    val first: Boolean
) : CoroutineStackFrame(frame, sourcePosition, emptyList(), false), XDebuggerFramesList.ItemWithSeparatorAbove {

    override fun getCaptionAboveOf() =
        KotlinDebuggerCoroutinesBundle.message("coroutine.dump.creation.trace")

    override fun hasSeparatorAbove() =
        first
}

open class CoroutineStackFrame(
    frame: StackFrameProxyImpl,
    val position: XSourcePosition?,
    private val spilledVariables: List<XNamedValue>? = null,
    private val includeFrameVariables: Boolean = true,
) : KotlinStackFrame(frame) {

    init {
        descriptor.updateRepresentation(null, DescriptorLabelListener.DUMMY_LISTENER)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val frame = other as? JavaStackFrame ?: return false

        return descriptor.frameProxy == frame.descriptor.frameProxy
    }

    override fun hashCode(): Int {
        return descriptor.frameProxy.hashCode()
    }

    override fun computeChildren(node: XCompositeNode) {
        if (includeFrameVariables || spilledVariables == null) {
            super.computeChildren(node)
        } else {
            // ignore original frame variables
            val list = XValueChildrenList()
            spilledVariables.forEach { list.add(it) }
            node.addChildren(list, true)
        }
    }

    override fun superBuildVariables(evaluationContext: EvaluationContextImpl, children: XValueChildrenList) {
        super.superBuildVariables(evaluationContext, children)
        if (spilledVariables != null) {
            children.let {
                val varNames = (0 until children.size()).map { children.getName(it) }.toSet()
                spilledVariables.forEach {
                    if (!varNames.contains(it.name))
                        children.add(it)
                }
            }
        }
    }

    override fun getSourcePosition() =
        position ?: super.getSourcePosition()
}