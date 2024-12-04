// CHECK_STATE_MACHINE
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: inlined.kt

interface SuspendRunnable {
    suspend fun run1()
    suspend fun run2()
}

suspend inline fun crossinlineMe(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) {
    val o = object : SuspendRunnable {
        override suspend fun run1() {
            c1()
            c1()
        }
        override suspend fun run2() {
            c2()
            c2()
        }
    }
    o.run1()
    o.run2()
}

// FILE: inlineSite.kt
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

var j = 0

suspend fun incrementJ() {
    j++
}

fun box(): String {
    StateMachineChecker.reset()
    builder {
        crossinlineMe({ StateMachineChecker.suspendHere() }) { incrementJ() }
    }
    if (j != 0) return "FAIL j != 0 $j"
    StateMachineChecker.check(numberOfSuspensions = 2)
    if (j != 2) return "FAIL j != 2 $j"
    return "OK"
}
