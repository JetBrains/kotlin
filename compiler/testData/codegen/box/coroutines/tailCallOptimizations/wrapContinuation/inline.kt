// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// API_VERSION: 2.4

// Using internal ModuleNameRetriever in stdlib replacement
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

// FILE: inline.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    x.resume(v)
    COROUTINE_SUSPENDED
}

inline suspend fun inlineMe1(): String {
    return suspendThere("OK1")
}

inline suspend fun inlineMe2(c: suspend () -> String): String {
    return c()
}

inline suspend fun inlineMe3(crossinline c: suspend () -> String): String {
    val o = object {
        suspend fun foo(): String {
            return c()
        }
    }
    return o.foo()
}

// FILE: test.kt
import helpers.*
import kotlin.coroutines.*

suspend fun suspendHere1(): String = inlineMe1()

suspend fun suspendHere2(): String = inlineMe2 {
    suspendThere("OK2")
}

suspend fun suspendHere3(): String = inlineMe3 {
    suspendThere("OK3")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere1()
    }
    TailCallOptimizationChecker.checkStateMachineIn("suspendHere1")
    if (result != "OK1") return "FAIL 1 $result"
    builder {
        result = suspendHere2()
    }
    TailCallOptimizationChecker.checkStateMachineIn("suspendHere2")
    if (result != "OK2") return "FAIL 2 $result"
    builder {
        result = suspendHere3()
    }
    TailCallOptimizationChecker.checkStateMachineIn("suspendHere3")
    if (result != "OK3") return "FAIL 3 $result"

    return "OK"
}

