// WITH_STDLIB
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*

inline fun inlineMe(crossinline c: suspend () -> Unit) = suspend { c(); c() }

inline fun inlineMe2(crossinline c: suspend () -> Unit) = inlineMe { c(); c() }

// FILE: box.kt
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    StateMachineChecker.reset()
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
