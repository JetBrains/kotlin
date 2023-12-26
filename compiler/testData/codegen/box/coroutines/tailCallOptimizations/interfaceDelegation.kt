// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

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

    suspend fun suspendHereNoTailCall(): String
}

class A : I {
    override suspend fun suspendHere(): String = suspendThere("OK")

    override suspend fun suspendHereNoTailCall(): String {
        suspendThere("FAIL 2")
        return "OK"
    }
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

    if (result != "OK") return "FAIL 1"

    builder {
        result = B(A()).suspendHereNoTailCall()
    }
    TailCallOptimizationChecker.checkStateMachineIn("suspendHereNoTailCall")
    TailCallOptimizationChecker.checkNoStateMachineIn("suspendHereNoTailCall\$suspendImpl")

    return result
}