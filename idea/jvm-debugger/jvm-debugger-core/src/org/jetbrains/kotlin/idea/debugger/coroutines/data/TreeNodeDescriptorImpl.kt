/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.data

import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MessageDescriptor
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.icons.AllIcons
import com.sun.jdi.ObjectReference
import javax.swing.Icon

@Deprecated("moved to XCoroutineView")
class CoroutineDescriptorImpl(val infoData: CoroutineInfoData) : NodeDescriptorImpl() {
    lateinit var icon: Icon

    override fun getName() = infoData.name

    @Throws(EvaluateException::class)
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener): String {
        val thread = infoData.activeThread
        val name = thread?.name()?.substringBefore(" @${infoData.name}") ?: ""
        val threadState = if (thread != null) DebuggerUtilsEx.getThreadStatusText(thread.status()) else ""
        val threadName = if (name.isNotEmpty()) " on thread \"$name\":$threadState" else ""
        return "${infoData.name}: ${infoData.state} $threadName"
    }

    override fun isExpandable() = infoData.state != CoroutineInfoData.State.CREATED

    private fun calcIcon() = when {
        infoData.isSuspended() -> AllIcons.Debugger.ThreadSuspended
        infoData.isCreated() -> AllIcons.Debugger.ThreadStates.Idle
        else -> AllIcons.Debugger.ThreadRunning
    }

    override fun setContext(context: EvaluationContextImpl?) {
        icon = calcIcon()
    }
}

@Deprecated("moved to XCoroutineView")
class CreationFramesDescriptor(val frames: List<StackTraceElement>) :
    MessageDescriptor("Coroutine creation stack trace", INFORMATION) {

    override fun isExpandable() = true
}

/**
 * Descriptor for suspend functions
 */
@Deprecated("moved to XCoroutineView")
class SuspendStackFrameDescriptor(
    val infoData: CoroutineInfoData,
    val frame: StackTraceElement,
    proxy: StackFrameProxyImpl,
    val continuation: ObjectReference
) :
    CoroutineStackFrameDescriptor(proxy) {
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener?): String {
        return with(frame) {
            val pack = className.substringBeforeLast(".", "")
            val packDisplay = if (pack.isNotEmpty()) "{$pack}" else ""
            "$methodName:$lineNumber, ${className.substringAfterLast(".")} $packDisplay"
        }
    }

    override fun getName() : String? = frame.methodName
}


/**
 * For the case when no data inside frame is available
 */
@Deprecated("moved to XCoroutineView")
class CoroutineCreatedStackFrameDescriptor(val frame: StackTraceElement, proxy: StackFrameProxyImpl) :
    CoroutineStackFrameDescriptor(proxy) {
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener?): String {
        return with(frame) {
            val pack = className.substringBeforeLast(".", "")
            val packDisplay = if (pack.isNotEmpty()) "{$pack}" else ""
            "$methodName:$lineNumber, ${className.substringAfterLast(".")} $packDisplay"
        }
    }

    override fun getName() = null
}

@Deprecated("moved to XCoroutineView")
class AsyncStackFrameDescriptor(val infoData: CoroutineInfoData, val frame: StackFrameItem, proxy: StackFrameProxyImpl) :
    CoroutineStackFrameDescriptor(proxy) {
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener?): String {
        return with(frame) {
            val pack = path().substringBeforeLast(".", "")
            val packDisplay = if (pack.isNotEmpty()) "{$pack}" else ""
            "${method()}:${line()}, ${path().substringAfterLast(".")} $packDisplay"
        }
    }

    override fun getName() = frame.method()
}

@Deprecated("moved to XCoroutineView")
open class CoroutineStackFrameDescriptor(proxy: StackFrameProxyImpl) : StackFrameDescriptorImpl(proxy, MethodsTracker()) {
    override fun isExpandable() = false
}
