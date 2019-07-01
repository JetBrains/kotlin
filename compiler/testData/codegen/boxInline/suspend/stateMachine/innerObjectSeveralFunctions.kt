// IGNORE_BACKEND: JVM_IR
// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

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
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

var j = 0

suspend fun incrementJ() {
    j++
}

fun box(): String {
    builder {
        crossinlineMe({ StateMachineChecker.suspendHere() }) { incrementJ() }
    }
    if (j != 0) return "FAIL j != 0 $j"
    StateMachineChecker.check(numberOfSuspensions = 2)
    if (j != 2) return "FAIL j != 2 $j"
    return "OK"
}