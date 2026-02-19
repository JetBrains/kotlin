// CHECK_STATE_MACHINE
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: inlined.kt

import kotlin.coroutines.intrinsics.*
import helpers.*

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = {
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
    }
    l()
    l()
}

// FILE: inlineSite.kt
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    StateMachineChecker.reset()
    var res = "OK"
    builder {
        crossinlineMe {
            res = "FAIL 1"
        }
    }
    StateMachineChecker.check(numberOfSuspensions = 10)
    return res
}
