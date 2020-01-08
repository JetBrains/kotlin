/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.descriptors.data.DescriptorData
import com.intellij.debugger.impl.descriptors.data.DisplayKey
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.openapi.project.Project
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.LookupContinuation
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

@Deprecated("moved to XCoroutineView")
class CoroutineStackTraceData(
    infoData: CoroutineInfoData,
    proxy: StackFrameProxyImpl,
    evalContext: EvaluationContextImpl,
    val frame: StackTraceElement
) : CoroutineStackDescriptorData(infoData, proxy, evalContext) {

    override fun hashCode() = frame.hashCode()

    override fun equals(other: Any?) =
        other is CoroutineStackTraceData && frame == other.frame

    override fun createDescriptorImpl(project: Project): NodeDescriptorImpl {
        val context = ExecutionContext(evalContext, proxy)
        val lookupContinuation = LookupContinuation(context, frame)

        // retrieve continuation only if suspend method
        val continuation = lookupContinuation.findContinuation(infoData)

        return if (continuation is ObjectReference)
            SuspendStackFrameDescriptor(infoData, frame, proxy, continuation)
        else
            CoroutineCreatedStackFrameDescriptor(frame, proxy)
    }
}

@Deprecated("moved to XCoroutineView")
class CoroutineStackFrameData(
    infoData: CoroutineInfoData,
    proxy: StackFrameProxyImpl,
    evalContext: EvaluationContextImpl,
    val frame: StackFrameItem
) :
    CoroutineStackDescriptorData(infoData, proxy, evalContext) {
    override fun createDescriptorImpl(project: Project): NodeDescriptorImpl =
        AsyncStackFrameDescriptor(infoData, frame, proxy)

    override fun hashCode() = frame.hashCode()

    override fun equals(other: Any?) = other is CoroutineStackFrameData && frame == other.frame
}

@Deprecated("moved to XCoroutineView")
abstract class CoroutineStackDescriptorData(
    val infoData: CoroutineInfoData,
    val proxy: StackFrameProxyImpl,
    val evalContext: EvaluationContextImpl
) : DescriptorData<NodeDescriptorImpl>() {

    override fun getDisplayKey(): DisplayKey<NodeDescriptorImpl> = SimpleDisplayKey(infoData)
}

/**
 * Describes coroutine itself in the tree (name: STATE), has children if stacktrace is not empty (state = CREATED)
 */
@Deprecated("moved to XCoroutineView")
class CoroutineDescriptorData(private val infoData: CoroutineInfoData) : DescriptorData<CoroutineDescriptorImpl>() {

    override fun createDescriptorImpl(project: Project) =
        CoroutineDescriptorImpl(infoData)

    override fun equals(other: Any?) = if (other !is CoroutineDescriptorData) false else infoData.name == other.infoData.name

    override fun hashCode() = infoData.name.hashCode()

    override fun getDisplayKey(): DisplayKey<CoroutineDescriptorImpl> = SimpleDisplayKey(infoData.name)
}