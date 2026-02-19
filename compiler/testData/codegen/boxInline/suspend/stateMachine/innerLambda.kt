// CHECK_STATE_MACHINE
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: inlined.kt

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = {
        c()
        c()
    }
    l()
    l()
}

// FILE: inlineSite.kt
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    StateMachineChecker.reset()
    builder {
        crossinlineMe {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    StateMachineChecker.check(numberOfSuspensions = 8)
    return "OK"
}
