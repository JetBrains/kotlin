/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.view

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.JavaExecutionStack
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.ui.DoubleClickListener
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ApplicationThreadExecutor
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ContinuationHolder
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.findPosition
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.suspendContextImpl
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

class XDebuggerTreeSelectedNodeListener(val session: XDebugSession, val tree: XDebuggerTree) {
    val applicationThreadExecutor = ApplicationThreadExecutor()
    val javaDebugProcess = session.debugProcess as JavaDebugProcess
    val debugProcess: DebugProcessImpl = javaDebugProcess.debuggerSession.process

    fun installOn() {
        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent) =
                nodeSelected(KeyMouseEvent(e))
        }.installOn(tree)

        tree.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    val key = e.keyCode
                    if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_RIGHT)
                        nodeSelected(KeyMouseEvent(e))
                }
            },
        )
    }

    fun nodeSelected(event: KeyMouseEvent): Boolean {
        val selectedNodes = tree.getSelectedNodes(XValueNodeImpl::class.java, null)
        if (selectedNodes.size == 1) {
            val node = selectedNodes[0]
            val valueContainer = node.valueContainer
            val suspendContext = session.suspendContextImpl()
            if (valueContainer is XCoroutineView.CoroutineFrameValue) {
                when (val stackFrameItem = valueContainer.frameItem) {
                    is RunningCoroutineStackFrameItem -> {
                        val threadProxy = stackFrameItem.frame.threadProxy()
                        val isCurrentContext = suspendContext.thread == threadProxy
                        val executionStack = JavaExecutionStack(
                            threadProxy,
                            debugProcess,
                            isCurrentContext
                        )
                        createStackAndSetFrame(threadProxy, { executionStack.createStackFrame(stackFrameItem.frame) }, isCurrentContext)
                    }
                    is CreationCoroutineStackFrameItem -> {
                        val position = stackFrameItem.stackTraceElement.findPosition(session.project) ?: return false
                        val threadProxy = suspendContext.thread ?: return false
                        createStackAndSetFrame(threadProxy, {
                            val realFrame = threadProxy.forceFrames().first() ?: return@createStackAndSetFrame null
                            SyntheticStackFrame(stackFrameItem.emptyDescriptor(realFrame), emptyList(), position)
                        })
                    }
                    is SuspendCoroutineStackFrameItem -> {
                        val threadProxy = suspendContext.thread ?: return false
                        val lastFrame = valueContainer.infoData.lastObservedFrameFieldRef ?: return false
                        createStackAndSetFrame(threadProxy, {
                            val realFrame = threadProxy.forceFrames().first() ?: return@createStackAndSetFrame null
                            createSyntheticStackFrame(suspendContext, stackFrameItem, realFrame, lastFrame)
                        })
                    }
                    is RestoredCoroutineStackFrameItem -> {
                        val threadProxy = stackFrameItem.frame.threadProxy()
                        val position = stackFrameItem.location.findPosition(session.project)
                            ?: return false
                        createStackAndSetFrame(threadProxy, {
                            SyntheticStackFrame(stackFrameItem.emptyDescriptor(), stackFrameItem.spilledVariables, position)
                        })
                    }
                    is DefaultCoroutineStackFrameItem -> {
                        val threadProxy = suspendContext.thread ?: return false
                        val position = stackFrameItem.location.findPosition(session.project)
                            ?: return false
                        createStackAndSetFrame(threadProxy, {
                            val realFrame = threadProxy.forceFrames().first() ?: return@createStackAndSetFrame null
                            SyntheticStackFrame(stackFrameItem.emptyDescriptor(realFrame), stackFrameItem.spilledVariables, position)
                        })
                    }
                    else -> {
                    }
                }
            }
        }
        return false
    }

    fun createStackAndSetFrame(
        threadReferenceProxy: ThreadReferenceProxyImpl,
        stackFrameProvider: () -> XStackFrame?,
        isCurrentContext: Boolean = false
    ) {
        val stackFrameStack = debugProcess.invokeInManagerThread {
            val stackFrame = stackFrameProvider.invoke() ?: return@invokeInManagerThread null
            XStackFrameStack(stackFrame, createExecutionStack(threadReferenceProxy, isCurrentContext))
        } ?: return
        setCurrentStackFrame(stackFrameStack)
    }

    fun setCurrentStackFrame(stackFrameStack: XStackFrameStack) {
        applicationThreadExecutor.schedule(
            {
                session.setCurrentStackFrame(stackFrameStack.executionStack, stackFrameStack.stackFrame)
            }, tree
        )
    }

    data class XStackFrameStack(val stackFrame: XStackFrame, val executionStack: XExecutionStack);

    private fun createExecutionStack(proxy: ThreadReferenceProxyImpl, isCurrentContext: Boolean = false): XExecutionStack {
        val executionStack = JavaExecutionStack(proxy, debugProcess, isCurrentContext)
        executionStack.initTopFrame()
        return executionStack
    }


    private fun createSyntheticStackFrame(
        suspendContext: SuspendContextImpl,
        frame: SuspendCoroutineStackFrameItem,
        topFrame: StackFrameProxyImpl,
        initialContinuation: ObjectReference
    ): SyntheticStackFrame? {
        val position =
            applicationThreadExecutor.readAction { frame.stackTraceElement.findPosition(session.project) }
                ?: return null
        val continuation =
            ContinuationHolder.lookup(suspendContext, initialContinuation)
                ?: return null

        return SyntheticStackFrame(
            frame.emptyDescriptor(topFrame),
            continuation.getSpilledVariables() ?: return null,
            position
        )
    }
}

data class KeyMouseEvent(val keyEvent: KeyEvent?, val mouseEvent: MouseEvent?) {
    constructor(keyEvent: KeyEvent) : this(keyEvent, null)
    constructor(mouseEvent: MouseEvent) : this(null, mouseEvent)

    fun isKeyEvent() = keyEvent != null

    fun isMouseEvent() = mouseEvent != null
}