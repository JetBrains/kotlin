// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

import COROUTINES_PACKAGE.intrinsics.*
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
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
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
    StateMachineChecker.check(numberOfSuspensions = 10)
    return res
}
