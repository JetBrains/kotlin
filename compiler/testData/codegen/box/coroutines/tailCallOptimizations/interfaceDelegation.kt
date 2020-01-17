// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    x.resume(v)
    COROUTINE_SUSPENDED
}

interface I {
    suspend fun suspendHere(): String
}

class A : I {
    override suspend fun suspendHere(): String = suspendThere("OK")
}

open class B(val x: I) : I by x // open override suspend fun suspendHere() = x.suspendHere()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = B(A()).suspendHere()
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("suspendHere")
    TailCallOptimizationChecker.checkNoStateMachineIn("suspendHere\$suspendImpl")

    return result
}
