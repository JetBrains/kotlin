// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
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
