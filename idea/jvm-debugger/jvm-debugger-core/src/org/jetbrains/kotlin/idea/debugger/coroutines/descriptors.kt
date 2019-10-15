/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.impl.descriptors.data.DescriptorData
import com.intellij.debugger.impl.descriptors.data.DisplayKey
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.sun.jdi.ClassType
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.IconExtensionChooser
import javaslang.control.Either
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import javax.swing.Icon

/**
 * Describes coroutine itself in the tree (name: STATE), has children if stacktrace is not empty (state = CREATED)
 */
class CoroutineData(private val state: CoroutineState) : DescriptorData<CoroutineDescriptorImpl>() {

    override fun createDescriptorImpl(project: Project): CoroutineDescriptorImpl {
        return CoroutineDescriptorImpl(state)
    }

    override fun equals(other: Any?) = if (other !is CoroutineData) false else state.name == other.state.name

    override fun hashCode() = state.name.hashCode()

    override fun getDisplayKey(): DisplayKey<CoroutineDescriptorImpl> = SimpleDisplayKey(state.name)
}

class CoroutineDescriptorImpl(val state: CoroutineState) : NodeDescriptorImpl() {
    var suspendContext: SuspendContextImpl? = null
    lateinit var icon: Icon

    override fun getName(): String? {
        return state.name
    }

    @Throws(EvaluateException::class)
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener): String {
        val name = if (state.thread != null) state.thread.name().substringBefore(" @${state.name}") else ""
        val threadState = if (state.thread != null) DebuggerUtilsEx.getThreadStatusText(state.thread.status()) else ""
        return "${state.name}: ${state.state}${if (name.isNotEmpty()) " on thread \"$name\":$threadState" else ""}"
    }

    override fun isExpandable(): Boolean {
        return state.state != CoroutineState.State.CREATED
    }

    private fun calcIcon() = when {
        state.isSuspended -> AllIcons.Debugger.ThreadSuspended
        state.state == CoroutineState.State.CREATED -> AllIcons.Debugger.ThreadStates.Idle
        else -> AllIcons.Debugger.ThreadRunning
    }

    override fun setContext(context: EvaluationContextImpl?) {
        icon = calcIcon()
    }
}

class CoroutineStackFrameData private constructor(val state: CoroutineState, private val proxy: StackFrameProxyImpl) :
    DescriptorData<NodeDescriptorImpl>() {
    private lateinit var frame: Either<StackTraceElement, StackFrameItem>

    constructor(state: CoroutineState, frame: StackTraceElement, proxy: StackFrameProxyImpl) : this(state, proxy) {
        this.frame = Either.left(frame)
    }

    constructor(state: CoroutineState, frameItem: StackFrameItem, proxy: StackFrameProxyImpl) : this(state, proxy) {
        this.frame = Either.right(frameItem)
    }

    override fun hashCode(): Int {
        return if (frame.isLeft) frame.left.hashCode() else frame.get().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is CoroutineStackFrameData && frame == other.frame
    }

    /**
     * Returns [EmptyStackFrameDescriptor], [SuspendStackFrameDescriptor]
     * or [AsyncStackFrameDescriptor] according to current frame
     */
    override fun createDescriptorImpl(project: Project): NodeDescriptorImpl {
        val isLeft = frame.isLeft
        if (!isLeft) return AsyncStackFrameDescriptor(state, frame.get(), proxy)
        // check whether last fun is suspend fun
        val frame = frame.left
        val suspendContext =
            DebuggerManagerEx.getInstanceEx(project).context.suspendContext ?: return EmptyStackFrameDescriptor(
                frame,
                proxy
            )
        val suspendProxy = suspendContext.frameProxy ?: return EmptyStackFrameDescriptor(
            frame,
            proxy
        )
        val evalContext = EvaluationContextImpl(suspendContext, suspendContext.frameProxy)
        val context = ExecutionContext(evalContext, suspendProxy)
        val clazz = context.findClass(frame.className) as ClassType
        val method = clazz.methodsByName(frame.methodName).last {
            val loc = it.location().lineNumber()
            loc < 0 && frame.lineNumber < 0 || loc > 0 && loc <= frame.lineNumber
        } // pick correct method if an overloaded one is given
        return if ("Lkotlin/coroutines/Continuation;)" in method.signature() ||
            method.name() == "invokeSuspend" &&
            method.signature() == "(Ljava/lang/Object;)Ljava/lang/Object;" // suspend fun or invokeSuspend
        ) {
            val continuation = state.getContinuation(frame, context)
            if (continuation == null) EmptyStackFrameDescriptor(
                frame,
                proxy
            ) else
                SuspendStackFrameDescriptor(
                    state,
                    frame,
                    proxy,
                    continuation
                )
        } else EmptyStackFrameDescriptor(frame, proxy)
    }

    override fun getDisplayKey(): DisplayKey<NodeDescriptorImpl> = SimpleDisplayKey(state)
}

/**
 * Descriptor for suspend functions
 */
class SuspendStackFrameDescriptor(
    val state: CoroutineState,
    val frame: StackTraceElement,
    proxy: StackFrameProxyImpl,
    val continuation: ObjectReference
) :
    StackFrameDescriptorImpl(proxy, MethodsTracker()) {
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener?): String {
        return with(frame) {
            val pack = className.substringBeforeLast(".", "")
            "$methodName:$lineNumber, ${className.substringAfterLast(".")} " +
                    if (pack.isNotEmpty()) "{$pack}" else ""
        }
    }

    override fun isExpandable() = false

    override fun getName(): String {
        return frame.methodName
    }
}

class AsyncStackFrameDescriptor(val state: CoroutineState, val frame: StackFrameItem, proxy: StackFrameProxyImpl) :
    StackFrameDescriptorImpl(proxy, MethodsTracker()) {
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener?): String {
        return with(frame) {
            val pack = path().substringBeforeLast(".", "")
            "${method()}:${line()}, ${path().substringAfterLast(".")} ${if (pack.isNotEmpty()) "{$pack}" else ""}"
        }
    }

    override fun getName(): String {
        return frame.method()
    }

    override fun isExpandable(): Boolean = false
}

/**
 * For the case when no data inside frame is available
 */
class EmptyStackFrameDescriptor(val frame: StackTraceElement, proxy: StackFrameProxyImpl) :
    StackFrameDescriptorImpl(proxy, MethodsTracker()) {
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener?): String {
        return with(frame) {
            val pack = className.substringBeforeLast(".", "")
            "$methodName:$lineNumber, ${className.substringAfterLast(".")} ${if (pack.isNotEmpty()) "{$pack}" else ""}"
        }
    }

    override fun getName() = null
    override fun isExpandable() = false
}

class CreationFramesDescriptor(val frames: List<StackTraceElement>) : NodeDescriptorImpl() {
    override fun calcRepresentation(context: EvaluationContextImpl?, labelListener: DescriptorLabelListener?) = name

    override fun setContext(context: EvaluationContextImpl?) {}
    override fun getName() = "Coroutine creation stack trace"
    override fun isExpandable() = true
}