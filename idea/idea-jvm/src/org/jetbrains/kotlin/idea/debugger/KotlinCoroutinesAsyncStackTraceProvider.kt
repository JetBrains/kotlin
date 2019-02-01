/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.AsyncStackTraceProvider
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
import org.jetbrains.kotlin.idea.debugger.evaluate.LOG
import org.jetbrains.kotlin.idea.debugger.evaluate.variables.VariableFinder.Companion.SUSPEND_LAMBDA_CLASSES
import org.jetbrains.kotlin.idea.debugger.evaluate.getInvokePolicy
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinCoroutinesAsyncStackTraceProvider : AsyncStackTraceProvider {
    private companion object {
        const val DEBUG_METADATA_KT = "kotlin.coroutines.jvm.internal.DebugMetadataKt"

        fun classByName(name: String, frame: StackFrameProxyImpl, threadReference: ThreadReference, invokePolicy: Int): ReferenceType? {
            val virtualMachine = frame.virtualMachine
            val classClass = virtualMachine.classesByName(Class::class.java.name).firstIsInstanceOrNull<ClassType>() ?: return null
            val forNameMethod = classClass.methodsByName("forName").first { it.signature() == "(Ljava/lang/String;)Ljava/lang/Class;" }

            try {
                val args = listOf(virtualMachine.mirrorOf(name))
                val result = classClass.invokeMethod(threadReference, forNameMethod, args, invokePolicy)

                if (result is ClassObjectReference) {
                    return result.reflectedType()
                }
            } catch (e: InvocationException) {
                // Ignore ClassNotFoundException
            } catch (e: Throwable) {
                LOG.error(e)
            }

            return null
        }

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
        val method = location.safeMethod() ?: return null
        val threadReference = frameProxy.threadProxy().threadReference.takeIf { it.isSuspended } ?: return null
        val invokePolicy = suspendContext.getInvokePolicy()
        val debugMetadataKtType = classByName(DEBUG_METADATA_KT, frameProxy, threadReference, invokePolicy) as? ClassType ?: return null

        val context = Context(suspendContext, frameProxy, method, debugMetadataKtType, threadReference, invokePolicy)
        return context.getAsyncStackTraceForSuspendLambda() ?: context.getAsyncStackTraceForSuspendFunction()
    }

    private fun Context.getAsyncStackTraceForSuspendLambda(): List<StackFrameItem>? {
        if (method.name() != "invokeSuspend" || method.signature() != "(Ljava/lang/Object;)Ljava/lang/Object;") {
            return null
        }

        val thisObject = frameProxy.thisObject() ?: return null
        val thisType = thisObject.referenceType()

        if (SUSPEND_LAMBDA_CLASSES.none { thisType.isSubtype(it) }) {
            return null
        }

        return collectFrames(thisObject)
    }

    private fun Context.getAsyncStackTraceForSuspendFunction(): List<StackFrameItem>? {
        if ("Lkotlin/coroutines/Continuation;)" !in method.signature()) {
            return null
        }

        val continuationVariable = frameProxy.safeVisibleVariableByName(CONTINUATION_VARIABLE_NAME) ?: return null
        val continuation = frameProxy.getValue(continuationVariable) as? ObjectReference ?: return null

        return collectFrames(continuation)
    }

    private fun Context.collectFrames(continuation: ObjectReference): List<StackFrameItem>? {
        val frames = mutableListOf<StackFrameItem>()
        collectFramesRecursively(continuation, frames)
        return frames
    }

    private fun Context.collectFramesRecursively(continuation: ObjectReference, consumer: MutableList<StackFrameItem>) {
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

    private class Context(
        val suspendContext: SuspendContextImpl,
        val frameProxy: StackFrameProxyImpl,
        val method: Method,
        private val debugMetadataKtType: ClassType,
        private val threadReference: ThreadReference,
        private val invokePolicy: Int
    ) {
        val evaluationContext: EvaluationContextImpl by lazy { EvaluationContextImpl(suspendContext, frameProxy) }

        fun getLocation(continuation: ObjectReference): Location? {
            val getStackTraceElementMethod = debugMetadataKtType.methodsByName(
                "getStackTraceElement",
                "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)Ljava/lang/StackTraceElement;"
            ).firstOrNull() ?: return null

            val args = listOf(continuation)

            val stackTraceElement = debugMetadataKtType
                .invokeMethod(threadReference, getStackTraceElementMethod, args, invokePolicy) as? ObjectReference ?: return null

            val stackTraceElementType = stackTraceElement.referenceType().takeIf { it.name() == StackTraceElement::class.java.name }
                ?: return null

            fun getValue(name: String, desc: String): Value? {
                val method = stackTraceElementType.methodsByName(name, desc).single()
                return stackTraceElement.invokeMethod(threadReference, method, emptyList(), invokePolicy)
            }

            val className = (getValue("getClassName", "()Ljava/lang/String;") as? StringReference)?.value() ?: return null
            val methodName = (getValue("getMethodName", "()Ljava/lang/String;") as? StringReference)?.value() ?: return null
            val lineNumber = (getValue("getLineNumber", "()I") as? IntegerValue)?.value()?.takeIf { it >= 0 } ?: return null

            val locationClass = classByName(className, frameProxy, threadReference, invokePolicy) ?: return null
            return GeneratedLocation(suspendContext.debugProcess, locationClass, methodName, lineNumber)
        }

        fun getSpilledVariables(continuation: ObjectReference): List<XNamedValue>? {
            val getSpilledVariableFieldMappingMethod = debugMetadataKtType.methodsByName(
                "getSpilledVariableFieldMapping",
                "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)[Ljava/lang/String;"
            ).firstOrNull() ?: return null

            val args = listOf(continuation)

            val rawSpilledVariables = debugMetadataKtType
                .invokeMethod(threadReference, getSpilledVariableFieldMappingMethod, args, invokePolicy) as? ArrayReference ?: return null

            val length = rawSpilledVariables.length() / 2
            val spilledVariables = ArrayList<XNamedValue>(length)

            for (index in 0 until length) {
                val fieldName = (rawSpilledVariables.getValue(2 * index) as? StringReference)?.value() ?: continue
                val variableName = (rawSpilledVariables.getValue(2 * index + 1) as? StringReference)?.value() ?: continue
                val field = continuation.referenceType().fieldByName(fieldName) ?: continue

                val project = suspendContext.debugProcess.project

                val valueDescriptor = object : ValueDescriptorImpl(project) {
                    override fun calcValueName() = variableName
                    override fun calcValue(evaluationContext: EvaluationContextImpl?) = continuation.getValue(field)
                    override fun getDescriptorEvaluation(context: DebuggerContext?) =
                        throw EvaluateException("Spilled variable evaluation is not supported")
                }

                spilledVariables += JavaValue.create(
                    null,
                    valueDescriptor,
                    evaluationContext,
                    suspendContext.debugProcess.xdebugProcess!!.nodeManager,
                    false
                )
            }

            return spilledVariables
        }
    }
}