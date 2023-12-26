// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun check() = true

suspend fun f_1(): Unit {
    return f_2()
}

private inline suspend fun f_2(): Unit {
    if (check()) return
    return suspendCoroutineUninterceptedOrReturn {
        TailCallOptimizationChecker.saveStackTrace(it)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder { f_1() }
    TailCallOptimizationChecker.checkNoStateMachineIn("f_1")
    return "OK"
}