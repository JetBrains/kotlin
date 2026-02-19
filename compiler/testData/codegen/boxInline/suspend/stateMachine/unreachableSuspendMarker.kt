// CHECK_STATE_MACHINE
// WITH_COROUTINES
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FULL_JDK
// WITH_STDLIB
// FILE: inline.kt

import helpers.*
import kotlin.coroutines.intrinsics.*

fun check() = true

inline suspend fun inlineMe(): Unit {
    suspendCoroutineUninterceptedOrReturn<Nothing> {
        if (check()) error("O") else error("Not this one")
    }
}

// FILE: box.kt

import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

suspend fun withoutTryCatch(): String {
    StateMachineChecker.suspendHere()
    inlineMe()
    return "" // To prevent tail-call optimization
}

fun box(): String {
    StateMachineChecker.reset()
    var result = "FAIL 0"
    builder {
        result = try {
            StateMachineChecker.suspendHere()
            inlineMe()
            "FAIL 1"
        } catch (e: IllegalStateException) {
            e.message!!
        }
        try {
            withoutTryCatch()
            result += "FAIL 2"
        } catch (e: IllegalStateException) {
            result += "K"
        }
    }
    StateMachineChecker.check(numberOfSuspensions = 2)
    return result
}
