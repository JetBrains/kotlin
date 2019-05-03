// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var p: Int = 5846814
private suspend fun withoutInline() {
    val c = { c: Continuation<Unit> ->
        TailCallOptimizationChecker.saveStackTrace(c)
        if (p > 52158) Unit else COROUTINE_SUSPENDED
    }

    return suspendCoroutineUninterceptedOrReturn(c)
}

private suspend fun withInline() {
    return suspendCoroutineUninterceptedOrReturn { c ->
        TailCallOptimizationChecker.saveStackTrace(c)
        if (p > 52158) Unit else COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        withoutInline()
        TailCallOptimizationChecker.checkNoStateMachineIn("withoutInline")
        withInline()
        TailCallOptimizationChecker.checkNoStateMachineIn("withInline")
    }
    return "OK"
}
