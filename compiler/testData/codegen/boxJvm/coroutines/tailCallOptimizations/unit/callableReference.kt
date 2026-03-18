// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun test(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    COROUTINE_SUSPENDED
}

suspend inline fun testInline() {
    test()
}

suspend inline fun testInline2() {
    test()
    test()
}

suspend fun testInt(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    COROUTINE_SUSPENDED
}

suspend fun coercionToUnit(sam: suspend () -> Unit) {
    sam()
}

suspend inline fun testInlineWithCoertion() {
    testInt()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        val c = ::test
        c()
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("invoke")

    builder {
        val c = ::testInline
        c()
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("invoke")

    builder {
        val c = ::testInline2
        c()
    }
    TailCallOptimizationChecker.checkStateMachineIn("invoke")

    builder {
        coercionToUnit(::testInt)
    }
    TailCallOptimizationChecker.checkStateMachineIn("invoke")

    builder {
        val c = ::testInlineWithCoertion
        c()
    }
    TailCallOptimizationChecker.checkStateMachineIn("invoke")
    return "OK"
}


