// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FULL_JDK
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION

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
