/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.util

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.sun.jdi.ClassType
import javaslang.control.Either
import org.jetbrains.kotlin.idea.debugger.coroutines.data.AsyncStackFrameDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.data.SuspendStackFrameDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.ApplicationThreadExecutor
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.LookupContinuation
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

fun getPosition(stackTraceElement: StackTraceElement, project: Project): XSourcePosition? {
    val psiFacade = JavaPsiFacade.getInstance(project)

    val psiClass = ApplicationThreadExecutor().invoke {
        @Suppress("DEPRECATION")
        psiFacade.findClass(
            stackTraceElement.className.substringBefore("$"), // find outer class, for which psi exists TODO
            GlobalSearchScope.everythingScope(project))
    }

    val classFile = psiClass?.containingFile?.virtualFile
    // to convert to 0-based line number or '-1' to do not move
    val lineNumber = if (stackTraceElement.lineNumber > 0) stackTraceElement.lineNumber - 1 else return null
    return XDebuggerUtil.getInstance().createPosition(classFile, lineNumber)
}


/*

private fun buildSuspendStackFrameChildren(descriptor: SuspendStackFrameDescriptor, frame: StackTraceElement, project: Project) {
    val context = DebuggerManagerEx.getInstanceEx(project).context
    val pos = getPosition(frame) ?: return
    context.debugProcess?.managerThread?.schedule(object : SuspendContextCommandImpl(context.suspendContext) {
        override fun contextAction() {
            val (stack, stackFrame) = createSyntheticStackFrame(descriptor, pos) ?: return
            val action: () -> Unit = { context.debuggerSession?.xDebugSession?.setCurrentStackFrame(stack, stackFrame) }
            ApplicationManager.getApplication()
                .invokeLater(action, ModalityState.stateForComponent(this@CoroutinesDebuggerTree))
        }
    })
}

private fun buildAsyncStackFrameChildren(descriptor: AsyncStackFrameDescriptor, process: DebugProcessImpl) {
    process.managerThread?.schedule(object : DebuggerCommandImpl() {
        override fun action() {
            val context = DebuggerManagerEx.getInstanceEx(project).context
            val proxy = ThreadReferenceProxyImpl(
                process.virtualMachineProxy,
                descriptor.state.thread // is not null because it's a running coroutine
            )
            val executionStack = JavaExecutionStack(proxy, process, false)
            executionStack.initTopFrame()
            val frame = descriptor.frame.createFrame(process)
            DebuggerUIUtil.invokeLater {
                context.debuggerSession?.xDebugSession?.setCurrentStackFrame(
                    executionStack,
                    frame
                )
            }
        }
    })
}

private fun buildEmptyStackFrameChildren(descriptor: EmptyStackFrameDescriptor, project: Project) {
    val position = getPosition(descriptor.frame) ?: return
    val context = DebuggerManagerEx.getInstanceEx(project).context
    val suspendContext = context.suspendContext ?: return
    val proxy = suspendContext.thread ?: return
    context.debugProcess?.managerThread?.schedule(object : DebuggerCommandImpl() {
        override fun action() {
            val executionStack =
                JavaExecutionStack(proxy, context.debugProcess!!, false)
            executionStack.initTopFrame()
            val frame = SyntheticStackFrame(descriptor, emptyList(), position)
            val action: () -> Unit =
                { context.debuggerSession?.xDebugSession?.setCurrentStackFrame(executionStack, frame) }
            ApplicationManager.getApplication()
                .invokeLater(action, ModalityState.stateForComponent(this@CoroutinesDebuggerTree))
        }
    })
}

*/

class EmptyStackFrameDescriptor(val frame: StackTraceElement, proxy: StackFrameProxyImpl) :
    StackFrameDescriptorImpl(proxy, MethodsTracker()) {
}
