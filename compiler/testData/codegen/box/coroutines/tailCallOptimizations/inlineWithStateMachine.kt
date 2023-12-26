// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

inline suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    x.resume(v)
    COROUTINE_SUSPENDED
}

// There's no state machine in the suspendHere, since it's inline
inline suspend fun suspendHere(): String = suspendThere("O") + suspendThere("K")
// There should be a state machine for mainSuspend as it has two suspend non-tail calls inlined
suspend fun mainSuspend() = suspendHere()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = mainSuspend()
    }

    TailCallOptimizationChecker.checkStateMachineIn("mainSuspend")

    return result
}
