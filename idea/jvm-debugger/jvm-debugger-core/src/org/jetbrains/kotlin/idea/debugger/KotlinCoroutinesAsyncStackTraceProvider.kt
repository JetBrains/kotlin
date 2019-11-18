/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

class KotlinCoroutinesAsyncStackTraceProvider : KotlinCoroutinesAsyncStackTraceProviderBase {
    private companion object {
        const val DEBUG_METADATA_KT = "kotlin.coroutines.jvm.internal.DebugMetadataKt"
    }


    override fun getAsyncStackTrace(stackFrame: JavaStackFrame, suspendContext: SuspendContextImpl): List<StackFrameItem>? {
        return hopelessAware { getAsyncStackTraceSafe(stackFrame.stackFrameProxy, suspendContext) }
    }

    fun getAsyncStackTraceSafe(frameProxy: StackFrameProxyImpl, suspendContext: SuspendContextImpl): List<StackFrameItem>? {
        val location = frameProxy.location()
        if (!location.isInKotlinSources()) {
            return null
        }

        val method = location.safeMethod() ?: return null
        val threadReference = frameProxy.threadProxy().threadReference

        if (threadReference == null || !threadReference.isSuspended || !suspendContext.debugProcess.canRunEvaluation) {
            return null
        }

        val context = createExecutionContext(suspendContext, frameProxy)

        // DebugMetadataKt not found, probably old kotlin-stdlib version
        val debugMetadataKtType = findDebugMetadata(context) ?: return null

        val asyncContext = AsyncStackTraceContext(context, method, debugMetadataKtType)
        return asyncContext.getAsyncStackTraceForSuspendLambda() ?: asyncContext.getAsyncStackTraceForSuspendFunction()
    }

    private fun findDebugMetadata(context: ExecutionContext): ClassType? = context.findClassSafe(DEBUG_METADATA_KT)

    private fun createExecutionContext(
        suspendContext: SuspendContextImpl,
        frameProxy: StackFrameProxyImpl
    ): ExecutionContext {
        val evaluationContext = EvaluationContextImpl(suspendContext, frameProxy)
        return ExecutionContext(evaluationContext, frameProxy)
    }
}

internal fun ExecutionContext.findClassSafe(className: String): ClassType? {
    return try {
        findClass(className) as? ClassType
    } catch (e: Throwable) {
        null
    }
}
