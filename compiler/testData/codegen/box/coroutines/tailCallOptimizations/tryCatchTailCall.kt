// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun catchException(): String {
    try {
        return suspendWithException()
    }
    catch(e: Exception) {
        return e.message!!
    }
}

suspend fun suspendWithException(): String = "OK".also { TailCallOptimizationChecker.saveStackTrace() }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = catchException()
    }
    TailCallOptimizationChecker.checkStateMachineIn("catchException")
    return res
}
