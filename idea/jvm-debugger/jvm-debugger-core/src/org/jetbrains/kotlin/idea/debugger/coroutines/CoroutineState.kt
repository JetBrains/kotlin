/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.isSubtype

/**
 * Represents state of a coroutine.
 * @see `kotlinx.coroutines.debug.CoroutineInfo`
 */
class CoroutineState(
    val name: String,
    val state: State,
    val thread: ThreadReference? = null,
    val stackTrace: List<StackTraceElement>,
    val frame: ObjectReference?
) {
    val isSuspended: Boolean = state == State.SUSPENDED
    val isEmptyStackTrace: Boolean by lazy { stackTrace.isEmpty() }
    val stringStackTrace: String by lazy {
        buildString {
            appendln("\"$name\", state: $state")
            stackTrace.forEach {
                appendln("\t$it")
            }
        }
    }

    /**
     * Finds previous Continuation for this Continuation (completion field in BaseContinuationImpl)
     * @return null if given ObjectReference is not a BaseContinuationImpl instance or completion is null
     */
    private fun getNextFrame(continuation: ObjectReference, context: ExecutionContext): ObjectReference? {
        val type = continuation.type() as ClassType
        if (!type.isSubtype("kotlin.coroutines.jvm.internal.BaseContinuationImpl")) return null
        val next = type.concreteMethodByName("getCompletion", "()Lkotlin/coroutines/Continuation;")
        return context.invokeMethod(continuation, next, emptyList()) as? ObjectReference
    }

    /**
     * Find continuation for the [stackTraceElement]
     * Gets current CoroutineInfo.lastObservedFrame and finds next frames in it until null or needed stackTraceElement is found
     * @return null if matching continuation is not found or is not BaseContinuationImpl
     */
    fun getContinuation(stackTraceElement: StackTraceElement, context: ExecutionContext): ObjectReference? {
        var continuation = frame ?: return null
        val baseType = "kotlin.coroutines.jvm.internal.BaseContinuationImpl"
        val getTrace = (continuation.type() as ClassType).concreteMethodByName(
            "getStackTraceElement",
            "()Ljava/lang/StackTraceElement;"
        )
        val stackTraceType = context.findClass("java.lang.StackTraceElement") as ClassType
        val getClassName = stackTraceType.concreteMethodByName("getClassName", "()Ljava/lang/String;")
        val getLineNumber = stackTraceType.concreteMethodByName("getLineNumber", "()I")
        val className = {
            val trace = context.invokeMethod(continuation, getTrace, emptyList()) as? ObjectReference
            if (trace != null)
                (context.invokeMethod(trace, getClassName, emptyList()) as StringReference).value()
            else null
        }
        val lineNumber = {
            val trace = context.invokeMethod(continuation, getTrace, emptyList()) as? ObjectReference
            if (trace != null)
                (context.invokeMethod(trace, getLineNumber, emptyList()) as IntegerValue).value()
            else null
        }

        while (continuation.type().isSubtype(baseType)
            && (stackTraceElement.className != className() || stackTraceElement.lineNumber != lineNumber())
        ) {
            // while continuation is BaseContinuationImpl and it's frame equals to the current
            continuation = getNextFrame(continuation, context) ?: return null
        }
        return if (continuation.type().isSubtype(baseType)) continuation else null
    }

    enum class State {
        RUNNING,
        SUSPENDED,
        CREATED
    }
}