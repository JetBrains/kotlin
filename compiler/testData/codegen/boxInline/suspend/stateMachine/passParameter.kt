// IGNORE_BACKEND: JVM_IR
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*

interface SuspendRunnable {
    suspend fun run()
    suspend fun run2()
}

inline fun inlineMe(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) =
    object : SuspendRunnable {
        override suspend fun run() {
            c1() // TODO: Double this call, when suspend markers are generated for inline and crossinline lambdas
        }

        override suspend fun run2() {
            c2()
        }
    }


inline fun inlineMe2(crossinline c1: suspend () -> Unit) =
    inlineMe(c1) {
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
    }

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
        r.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 2)
    StateMachineChecker.reset()
    builder {
        r.run2()
    }
    StateMachineChecker.check(numberOfSuspensions = 2)
    return "OK"
}
