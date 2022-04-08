// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendFun(x: String): String = suspendCoroutineUninterceptedOrReturn {
    TailCallOptimizationChecker.saveStackTrace(it)
    COROUTINE_SUSPENDED
}

suspend fun myFunWithTailCall(x: String) {
    x.let { suspendFun(it) }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        myFunWithTailCall("...")
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("myFunWithTailCall")
    return "OK"
}
