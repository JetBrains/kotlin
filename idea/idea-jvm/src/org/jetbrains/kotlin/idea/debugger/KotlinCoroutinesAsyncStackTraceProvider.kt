/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.xdebugger.frame.XNamedValue
import com.sun.jdi.*
import org.jetbrains.kotlin.codegen.coroutines.CONTINUATION_VARIABLE_NAME
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.evaluate.variables.VariableFinder.Companion.SUSPEND_LAMBDA_CLASSES

class KotlinCoroutinesAsyncStackTraceProvider : KotlinCoroutinesAsyncStackTraceProviderBase {
    private companion object {
        const val DEBUG_METADATA_KT = "kotlin.coroutines.jvm.internal.DebugMetadataKt"

        tailrec fun findBaseContinuationSuperSupertype(type: ClassType): ClassType? {
            if (type.name() == "kotlin.coroutines.jvm.internal.BaseContinuationImpl") {
                return type
            }

            return findBaseContinuationSuperSupertype(type.superclass() ?: return null)
        }
    }

    override fun getAsyncStackTrace(stackFrame: JavaStackFrame, suspendContext: SuspendContextImpl): List<StackFrameItem>? {
        return getAsyncStackTrace(stackFrame.stackFrameProxy, suspendContext)
    }

    fun getAsyncStackTrace(frameProxy: StackFrameProxyImpl, suspendContext: SuspendContextImpl): List<StackFrameItem>? {
        val location = frameProxy.location()
        if (!location.isInKotlinSources()) {
            return null
        }

        val method = location.safeMethod() ?: return null
        val threadReference = frameProxy.threadProxy().threadReference

        if (threadReference == null || !threadReference.isSuspended || !suspendContext.debugProcess.canRunEvaluation) {
            return null
        }

        val evaluationContext = EvaluationContextImpl(suspendContext, frameProxy)
        val context = ExecutionContext(evaluationContext, frameProxy)

        // DebugMetadataKt not found, probably old kotlin-stdlib version
        val debugMetadataKtType = context.findClassSafe(DEBUG_METADATA_KT) ?: return null

        val asyncContext = AsyncStackTraceContext(context, method, debugMetadataKtType)
        return asyncContext.getAsyncStackTraceForSuspendLambda() ?: asyncContext.getAsyncStackTraceForSuspendFunction()
    }

    private fun AsyncStackTraceContext.getAsyncStackTraceForSuspendLambda(): List<StackFrameItem>? {
        if (method.name() != "invokeSuspend" || method.signature() != "(Ljava/lang/Object;)Ljava/lang/Object;") {
            return null
        }

        val thisObject = context.frameProxy.thisObject() ?: return null
        val thisType = thisObject.referenceType()

        if (SUSPEND_LAMBDA_CLASSES.none { thisType.isSubtype(it) }) {
            return null
        }

        return collectFrames(thisObject)
    }

    private fun AsyncStackTraceContext.getAsyncStackTraceForSuspendFunction(): List<StackFrameItem>? {
        if ("Lkotlin/coroutines/Continuation;)" !in method.signature()) {
            return null
        }

        val frameProxy = context.frameProxy
        val continuationVariable = frameProxy.safeVisibleVariableByName(CONTINUATION_VARIABLE_NAME) ?: return null
        val continuation = frameProxy.getValue(continuationVariable) as? ObjectReference ?: return null

        return collectFrames(continuation)
    }

    private fun AsyncStackTraceContext.collectFrames(continuation: ObjectReference): List<StackFrameItem>? {
        val frames = mutableListOf<StackFrameItem>()
        collectFramesRecursively(continuation, frames)
        return frames
    }

    private fun AsyncStackTraceContext.collectFramesRecursively(continuation: ObjectReference, consumer: MutableList<StackFrameItem>) {
        val continuationType = continuation.referenceType() as? ClassType ?: return
        val baseContinuationSupertype = findBaseContinuationSuperSupertype(continuationType) ?: return

        val location = getLocation(continuation)
        val spilledVariables = getSpilledVariables(continuation) ?: emptyList()

        if (location != null) {
            consumer += StackFrameItem(location, spilledVariables)
        }

        val completionField = baseContinuationSupertype.fieldByName("completion") ?: return
        val completion = continuation.getValue(completionField) as? ObjectReference ?: return
        collectFramesRecursively(completion, consumer)
    }

    private fun AsyncStackTraceContext.getLocation(continuation: ObjectReference): Location? {
        val getStackTraceElementMethod = debugMetadataKtType.methodsByName(
            "getStackTraceElement",
            "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)Ljava/lang/StackTraceElement;"
        ).firstOrNull() ?: return null

        val args = listOf(continuation)

        val stackTraceElement = context.invokeMethod(debugMetadataKtType, getStackTraceElementMethod, args) as? ObjectReference
            ?: return null

        val stackTraceElementType = stackTraceElement.referenceType().takeIf { it.name() == StackTraceElement::class.java.name }
            ?: return null

        fun getValue(name: String, desc: String): Value? {
            val method = stackTraceElementType.methodsByName(name, desc).single()
            return context.invokeMethod(stackTraceElement, method, emptyList())
        }

        val className = (getValue("getClassName", "()Ljava/lang/String;") as? StringReference)?.value() ?: return null
        val methodName = (getValue("getMethodName", "()Ljava/lang/String;") as? StringReference)?.value() ?: return null
        val lineNumber = (getValue("getLineNumber", "()I") as? IntegerValue)?.value()?.takeIf { it >= 0 } ?: return null

        val locationClass = context.findClassSafe(className) ?: return null
        return GeneratedLocation(context.debugProcess, locationClass, methodName, lineNumber)
    }

    private fun AsyncStackTraceContext.getSpilledVariables(continuation: ObjectReference): List<XNamedValue>? {
        val getSpilledVariableFieldMappingMethod = debugMetadataKtType.methodsByName(
            "getSpilledVariableFieldMapping",
            "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)[Ljava/lang/String;"
        ).firstOrNull() ?: return null

        val args = listOf(continuation)

        val rawSpilledVariables = context.invokeMethod(debugMetadataKtType, getSpilledVariableFieldMappingMethod, args) as? ArrayReference
            ?: return null

        val length = rawSpilledVariables.length() / 2
        val spilledVariables = ArrayList<XNamedValue>(length)

        for (index in 0 until length) {
            val fieldName = (rawSpilledVariables.getValue(2 * index) as? StringReference)?.value() ?: continue
            val variableName = (rawSpilledVariables.getValue(2 * index + 1) as? StringReference)?.value() ?: continue
            val field = continuation.referenceType().fieldByName(fieldName) ?: continue

            val valueDescriptor = object : ValueDescriptorImpl(context.project) {
                override fun calcValueName() = variableName
                override fun calcValue(evaluationContext: EvaluationContextImpl?) = continuation.getValue(field)
                override fun getDescriptorEvaluation(context: DebuggerContext?) =
                    throw EvaluateException("Spilled variable evaluation is not supported")
            }

            spilledVariables += JavaValue.create(
                null,
                valueDescriptor,
                context.evaluationContext,
                context.debugProcess.xdebugProcess!!.nodeManager,
                false
            )
        }

        return spilledVariables
    }

    private fun ExecutionContext.findClassSafe(className: String): ClassType? {
        return try {
            findClass(className) as? ClassType
        } catch (e: Throwable) {
            null
        }
    }

    private class AsyncStackTraceContext(
        val context: ExecutionContext,
        val method: Method,
        val debugMetadataKtType: ClassType
    )
}