// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*

inline fun inlineMe(crossinline c: suspend () -> Unit) = suspend { c(); c() }

inline fun inlineMe2(crossinline c: suspend () -> Unit) = inlineMe { c(); c() }

// FILE: box.kt
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    val r = inlineMe2 {
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
    }

    builder {
        r()
    }
    StateMachineChecker.check(numberOfSuspensions = 8)
    return "OK"
}
