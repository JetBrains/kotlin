/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.xdebugger.frame.XSuspendContext
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineAsyncStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.AsyncStackTraceContext
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

class CoroutineAsyncStackTraceProvider : AsyncStackTraceProvider {

    override fun getAsyncStackTrace(stackFrame: JavaStackFrame, suspendContext: SuspendContextImpl): List<CoroutineAsyncStackFrameItem>? {
        val stackFrameList = hopelessAware { getAsyncStackTraceSafe(stackFrame.stackFrameProxy, suspendContext) } ?: emptyList()
        return null
    }

    fun getAsyncStackTrace(stackFrame: JavaStackFrame, suspendContext: XSuspendContext): List<CoroutineAsyncStackFrameItem>? {
        val stackFrameList = hopelessAware { getAsyncStackTraceSafe(stackFrame.stackFrameProxy, suspendContext) } ?: emptyList()
        return null
    }

    fun getAsyncStackTraceSafe(frameProxy: StackFrameProxyImpl, suspendContext: XSuspendContext): List<CoroutineAsyncStackFrameItem> {
        val defaultResult = emptyList<CoroutineAsyncStackFrameItem>()

        val location = frameProxy.location()
        if (!location.isInKotlinSources())
            return defaultResult

        val method = location.safeMethod() ?: return defaultResult
        val threadReference = frameProxy.threadProxy().threadReference

        if (threadReference == null || !threadReference.isSuspended || !canRunEvaluation(suspendContext))
            return defaultResult


        val astContext = createAsyncStackTraceContext(frameProxy, suspendContext, method)
        return astContext.getAsyncStackTraceIfAny()
    }

    private fun createAsyncStackTraceContext(
        frameProxy: StackFrameProxyImpl,
        suspendContext: XSuspendContext,
        method: Method
    ): AsyncStackTraceContext {
        val evaluationContext = EvaluationContextImpl(suspendContext as SuspendContextImpl, frameProxy)
        val context = ExecutionContext(evaluationContext, frameProxy)
        // DebugMetadataKt not found, probably old kotlin-stdlib version
        return AsyncStackTraceContext(context, method)
    }

    fun canRunEvaluation(suspendContext: XSuspendContext) =
        (suspendContext as SuspendContextImpl).debugProcess.canRunEvaluation
}

