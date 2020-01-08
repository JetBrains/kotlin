/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.isSubtype

class LookupContinuation(val context: ExecutionContext, val frame: StackTraceElement) {

    private fun suspendOrInvokeSuspend(method: Method): Boolean =
        "Lkotlin/coroutines/Continuation;)" in method.signature() ||
        (method.name() == "invokeSuspend" && method.signature() == "(Ljava/lang/Object;)Ljava/lang/Object;") // suspend fun or invokeSuspend

    private fun findMethod() : Method {
        val clazz = context.findClass(frame.className) as ClassType
        val method = clazz.methodsByName(frame.methodName).last {
            val loc = it.location().lineNumber()
            loc < 0 && frame.lineNumber < 0 || loc > 0 && loc <= frame.lineNumber
        } // pick correct method if an overloaded one is given
        return method
    }

    fun isApplicable(): Boolean {
        val method = findMethod()
        return suspendOrInvokeSuspend(method)
    }

    /**
     * Find continuation for the [frame]
     * Gets current CoroutineInfo.lastObservedFrame and finds next frames in it until null or needed stackTraceElement is found
     * @return null if matching continuation is not found or is not BaseContinuationImpl
     */
    fun findContinuation(infoData: CoroutineInfoData): ObjectReference? {
        if (!isApplicable())
            return null

        var continuation = infoData.frame ?: return null
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
            && (frame.className != className() || frame.lineNumber != lineNumber())
        ) {
            // while continuation is BaseContinuationImpl and it's frame equals to the current
            continuation = getNextFrame(continuation, context) ?: return null
        }
        return if (continuation.type().isSubtype(baseType)) continuation else null
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
}