// WITH_STDLIB
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*

inline class IC(val s: String)

suspend fun o(): IC {
    StateMachineChecker.suspendHere()
    return IC("O")
}

suspend fun k(): IC {
    StateMachineChecker.suspendHere()
    return IC("K")
}

inline suspend fun inlineMe(): String {
    return o().s + k().s
}

// FILE: box.kt

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = inlineMe()
    }
    StateMachineChecker.check(2)
    return res
}
