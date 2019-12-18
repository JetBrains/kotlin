/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.proxy

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.impl.PrioritizedTask
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.Computable
import com.intellij.xdebugger.frame.XSuspendContext
import java.awt.Component

class ManagerThreadExecutor(val debugProcess: DebugProcessImpl) {
    fun on(suspendContext: SuspendContextImpl, priority: PrioritizedTask.Priority = PrioritizedTask.Priority.NORMAL) =
        ManagerThreadExecutorInstance(suspendContext, priority)

    fun on(suspendContext: XSuspendContext, priority: PrioritizedTask.Priority = PrioritizedTask.Priority.NORMAL) =
        ManagerThreadExecutorInstance(suspendContext as SuspendContextImpl, priority)

    inner class ManagerThreadExecutorInstance(
        val suspendContext: SuspendContextImpl,
        val priority: PrioritizedTask.Priority = PrioritizedTask.Priority.NORMAL
    ) {

        fun schedule(f: (SuspendContextImpl) -> Unit) {
            val suspendContextCommand = object : SuspendContextCommandImpl(suspendContext) {
                override fun getPriority() = this@ManagerThreadExecutorInstance.priority

                override fun contextAction(suspendContext: SuspendContextImpl) {
                    f(suspendContext)
                }
            }
            debugProcess.managerThread.invoke(suspendContextCommand)
        }
    }
}

class ApplicationThreadExecutor {
    fun <T> readAction(f: () -> T): T {
        return ApplicationManager.getApplication().runReadAction(Computable(f))
    }

    fun schedule(f: () -> Unit, component: Component) {
        return ApplicationManager.getApplication().invokeLater( { f() }, ModalityState.stateForComponent(component))
    }

    fun schedule(f: () -> Unit) {
        return ApplicationManager.getApplication().invokeLater { f() }
    }
}
