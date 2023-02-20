// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION

import kotlin.coroutines.*
import helpers.*

suspend fun funThatShouldntAffectTco(): Nothing {
    TailCallOptimizationChecker.saveStackTrace()
    suspendCoroutine<Unit> {}
    error("BOOYA")
}

fun check(): Boolean = true

inline fun sendImpl(block: () -> Unit): Unit {
    if (check()) {
        return block()
    }
    if (check()) {
        return block()
    }
}

suspend fun test() {
    sendImpl(block = { funThatShouldntAffectTco() })
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        test()
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("test")
    return "OK"
}
