// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// LANGUAGE: +WrapContinuationForTailCallFunctions

// Using internal TailCallBaseContinuationImpl in stdlib replacement
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR
// PREFER_IN_TEST_OVER_STDLIB

// FILE: TailCallAsyncStackTraceEntry.kt

package kotlin.coroutines.jvm.internal

import kotlin.coroutines.*

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
internal fun <T> wrapContinuation(
    declaringClass: String, methodName: String, fileName: String, lineNumber: Int,
    spilledVariables: Array<Any?>,
    continuation: T,
): T where T : Continuation<Any?>, T : CoroutineStackFrame {
    return TailCallBaseContinuationImpl(
        declaringClass, methodName, fileName, lineNumber, spilledVariables, continuation
    ) as T
}

// FILE: test.kt
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package some.llong.name

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

class Test {
    suspend fun getStackTraceElement(): StackTraceElement {
        return suspendCoroutineUninterceptedOrReturn<StackTraceElement> {
            (it as BaseContinuationImpl).getStackTraceElement()
        }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "OK"
    builder {
        if (Test().getStackTraceElement().className != "some.llong.name.Test") {
            res = Test().getStackTraceElement().className
        }
    }
    return res
}
