/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.debugger.actions.AsyncStacksToggleAction
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.debugger.StackFrameInterceptor
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ContinuationHolder
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.SkipCoroutineStackFrameProxyImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CoroutineFrameBuilder
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isInUnitTest

class CoroutineStackFrameInterceptor(val project: Project) : StackFrameInterceptor {
    override fun createStackFrame(frame: StackFrameProxyImpl, debugProcess: DebugProcessImpl, location: Location): XStackFrame? {
        val stackFrame = if (debugProcess.xdebugProcess?.session is XDebugSessionImpl
            && frame !is SkipCoroutineStackFrameProxyImpl
            && AsyncStacksToggleAction.isAsyncStacksEnabled(debugProcess.xdebugProcess?.session as XDebugSessionImpl)
        ) {
            val suspendContextImpl = when {
                isInUnitTest() -> debugProcess.suspendManager.pausedContext
                else -> debugProcess.debuggerContext.suspendContext
            }
            if (!isInUnitTest())
                assert(debugProcess.suspendManager.pausedContext === debugProcess.debuggerContext.suspendContext)
            suspendContextImpl?.let {
                CoroutineFrameBuilder.coroutineExitFrame(frame, it)
            }
        } else
            null
        return stackFrame
    }
}