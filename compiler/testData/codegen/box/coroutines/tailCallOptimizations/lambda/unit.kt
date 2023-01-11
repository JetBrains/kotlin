// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var result = ""

suspend fun suspendHere(): String =
    suspendCoroutineUninterceptedOrReturn { x ->
        TailCallOptimizationChecker.saveStackTrace(x)
        result = "OK"
        x.resume(result)
        COROUTINE_SUSPENDED
    }

fun returnsLambda(): suspend () -> Unit = {
    suspendHere()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        returnsLambda()()
    }
    TailCallOptimizationChecker.checkStateMachineIn("invokeSuspend", "returnsLambda$1")
    TailCallOptimizationChecker.checkNoStateMachineIn("invoke", "returnsLambda$1")

    return result
}
