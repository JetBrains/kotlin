// IGNORE_BACKEND: JVM_IR
// FILE: inline.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// FULL_JDK
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

import helpers.*
import COROUTINES_PACKAGE.intrinsics.*

fun check() = true

inline suspend fun inlineMe(): Unit {
    suspendCoroutineUninterceptedOrReturn<Nothing> {
        if (check()) error("O") else error("Not this one")
    }
}

// FILE: box.kt
// WITH_COROUTINES

import COROUTINES_PACKAGE.*
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
