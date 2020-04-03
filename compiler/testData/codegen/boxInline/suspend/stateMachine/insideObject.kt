// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*

interface SuspendRunnable {
    suspend fun run()
}

class R : SuspendRunnable {
    override suspend fun run() {
        val sr: SuspendRunnable = inlineMe2 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
        sr.run()
    }

    inline fun inlineMe2(crossinline c: suspend () -> Unit) = object : SuspendRunnable {
        override suspend fun run() {
            c()
            c()
        }
    }
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    builder {
        R().run()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    return "OK"
}
