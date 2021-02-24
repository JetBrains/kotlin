/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.impl.PrioritizedTask
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.Computable
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.XSuspendContext
import java.awt.Component

class ManagerThreadExecutor(val debugProcess: DebugProcessImpl) {

    constructor(session: XDebugSession) : this(session.debugProcess)

    constructor(debugProcess: XDebugProcess) : this((debugProcess as JavaDebugProcess).debuggerSession.process)

    fun on(suspendContext: XSuspendContext, priority: PrioritizedTask.Priority = PrioritizedTask.Priority.NORMAL) =
        ManagerThreadExecutorInstance(suspendContext, priority)

    inner class ManagerThreadExecutorInstance(
        val suspendContext: SuspendContextImpl,
        val priority: PrioritizedTask.Priority = PrioritizedTask.Priority.NORMAL
    ) {

        constructor(sc: XSuspendContext, priority: PrioritizedTask.Priority) : this(sc as SuspendContextImpl, priority)

        fun invoke(f: (SuspendContextImpl) -> Unit) {
            debugProcess.managerThread.invoke(makeCommand(f))
        }

        private fun makeCommand(f: (SuspendContextImpl) -> Unit) =
            object : SuspendContextCommandImpl(suspendContext) {
                override fun getPriority() = this@ManagerThreadExecutorInstance.priority

                override fun contextAction(suspendContext: SuspendContextImpl) {
                    f(suspendContext)
                }
            }
    }
}

fun invokeLater(component: Component, f: () -> Unit) =
    ApplicationManager.getApplication().invokeLater({ f() }, ModalityState.stateForComponent(component))
