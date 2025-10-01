// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// API_VERSION: LATEST

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

// FILE: test.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

private suspend fun foo(): Int {
    suspendThere()
    //Breakpoint!
    return 42
}

suspend fun first(): Int { // tail-call opt, no state machine, wrapContinuation is invoked
    val i = 0
    val a = "OK"
    return foo()
}

var c: Continuation<Any?>? = null

suspend fun suspendThere(): Any? = suspendCoroutineUninterceptedOrReturn { x ->
    c = x
    TailCallOptimizationChecker.saveStackTrace(x)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        first()
    }
    TailCallOptimizationChecker.checkStateMachineIn("first")

    var firstHit = false

    while (c != null) {
        val cc = c
        if (cc is TailCallBaseContinuationImpl && cc.methodName == "first") {
            firstHit = true
            if (cc.spilledVariables[0] != "i") return "FAIL 0 ${cc.spilledVariables[0]}"
            if (cc.spilledVariables[1] != 0) return "FAIL 1 ${cc.spilledVariables[1]}"
            if (cc.spilledVariables[2] != "a") return "FAIL 2 ${cc.spilledVariables[2]}"
            if (cc.spilledVariables[3] != "OK") return "FAIL 3 ${cc.spilledVariables[3]}"
        }
        c = (c as? BaseContinuationImpl)?.completion
    }

    if (!firstHit) return "FAIL 4 'first' is not present in completion chain"

    return "OK"
}
