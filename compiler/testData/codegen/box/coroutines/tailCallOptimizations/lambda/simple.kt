// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(v: String): String =
    suspendCoroutineUninterceptedOrReturn { x ->
        TailCallOptimizationChecker.saveStackTrace(x)
        x.resume(v)
        COROUTINE_SUSPENDED
    }

fun returnsLambda(): suspend (String) -> String = {
    suspendHere(it)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = returnsLambda()("OK")
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("invokeSuspend", "returnsLambda$1")
    TailCallOptimizationChecker.checkNoStateMachineIn("invoke", "returnsLambda$1")

    return result
}
