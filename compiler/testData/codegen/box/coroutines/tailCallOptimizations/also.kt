// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun dummy() = TailCallOptimizationChecker.saveStackTrace()
suspend fun test(): Int = 1.also {
    dummy()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = 0
    builder {
        res = test()
    }
    TailCallOptimizationChecker.checkStateMachineIn("test")
    return if (res == 1) "OK" else "FAIL"
}
