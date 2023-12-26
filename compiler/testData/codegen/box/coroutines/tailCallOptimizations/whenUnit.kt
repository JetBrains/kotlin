// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

sealed class X {
    class A : X()
    class B : X()
}

var log = ""

suspend fun process(a: X.A) {
    log = "${log}from A;"
    TailCallOptimizationChecker.saveStackTrace()
}

suspend fun process(b: X.B) {
    log = "${log}from B;"
    TailCallOptimizationChecker.saveStackTrace()
}

suspend fun process(x: X) = when (x) {
    is X.A -> process(x)
    is X.B -> process(x)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        process(X.A())
        TailCallOptimizationChecker.checkNoStateMachineIn("process")
        process(X.B())
        TailCallOptimizationChecker.checkNoStateMachineIn("process")
    }
    if (log != "from A;from B;") return log
    return "OK"
}