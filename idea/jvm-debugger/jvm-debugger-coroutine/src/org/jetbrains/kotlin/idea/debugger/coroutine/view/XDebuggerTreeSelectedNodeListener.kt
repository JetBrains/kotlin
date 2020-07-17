/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.view

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.JavaExecutionStack
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.ui.DoubleClickListener
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.invokeLater
import org.jetbrains.kotlin.idea.debugger.coroutine.util.suspendContextImpl
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import org.jetbrains.kotlin.idea.decompiler.classFile.isKotlinInternalCompiledFile
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

class XDebuggerTreeSelectedNodeListener(val session: XDebugSession, val tree: XDebuggerTree) {
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
                val frameItem = valueContainer.frameItem
                val frame = frameItem.createFrame(debugProcess) ?: return false

                val executionStack =
                    if (frameItem is RunningCoroutineStackFrameItem)
                        createExecutionStack(frameItem.frame.threadProxy(), debugProcess)
                    else
                        suspendContext.thread?.let {
                            createExecutionStack(it, debugProcess)
                        }

                if (executionStack != null)
                    setCurrentStackFrame(executionStack, frame)

            }
        }
        return false
    }

    fun setCurrentStackFrame(executionStack: XExecutionStack, stackFrame: XStackFrame) {
        val fileToNavigate = stackFrame.sourcePosition?.file ?: return
        val isKotlinInternalCompiledFile = isKotlinInternalCompiledFile(fileToNavigate)
        if (!isKotlinInternalCompiledFile) {
            invokeLater(tree) {
                session.setCurrentStackFrame(executionStack, stackFrame, false)
            }
        }
    }
}

fun createExecutionStack(threadReference: ThreadReferenceProxyImpl, debugProcess: DebugProcessImpl): JavaExecutionStack? =
    debugProcess.invokeInManagerThread {
        val executionStack = JavaExecutionStack(threadReference, debugProcess, debugProcess.isSameContext(threadReference))
        executionStack.initTopFrame()
        executionStack
    }

data class KeyMouseEvent(val keyEvent: KeyEvent?, val mouseEvent: MouseEvent?) {
    constructor(keyEvent: KeyEvent) : this(keyEvent, null)
    constructor(mouseEvent: MouseEvent) : this(null, mouseEvent)

    fun isKeyEvent() = keyEvent != null

    fun isMouseEvent() = mouseEvent != null
}

fun DebugProcessImpl.isSameContext(threadReference: ThreadReferenceProxyImpl): Boolean =
    suspendManager.pausedContext.thread == threadReference