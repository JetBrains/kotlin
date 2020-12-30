// FILE: inlined.kt
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

import kotlin.coroutines.intrinsics.*
import helpers.*

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val o = object : SuspendRunnable {
        override suspend fun run() {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    o.run()
}

// FILE: inlineSite.kt
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    var res = "OK"
    builder {
        crossinlineMe {
            res = "FAIL 1"
        }
    }
    StateMachineChecker.check(numberOfSuspensions = 5)
    return res
}
