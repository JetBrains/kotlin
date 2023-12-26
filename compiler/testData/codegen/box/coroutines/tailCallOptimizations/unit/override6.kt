// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*

var c: Continuation<*>? = null

suspend fun suspendHere() = TailCallOptimizationChecker.saveStackTrace()

interface Base<T> {
    suspend fun generic(): T
}

inline fun inlineMe(crossinline c: suspend () -> Unit) = object : Base<Unit> {
    override suspend fun generic(): Unit {
        c()
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        inlineMe { suspendHere() }.generic()
    }

    TailCallOptimizationChecker.checkStateMachineIn("generic")

    return "OK"
}
