// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*
import COROUTINES_PACKAGE.*

interface SuspendRunnable {
    suspend fun run()
}

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

inline suspend fun inlineMe(crossinline c1: suspend () -> Unit) {
    object : SuspendRunnable {
        override suspend fun run() {
            c1()
        }
    }.run()

    StateMachineChecker.check(2)
    StateMachineChecker.reset()

    runSuspend {
        object : SuspendRunnable {
            override suspend fun run() {
                StateMachineChecker.suspendHere()
                StateMachineChecker.suspendHere()
            }
        }.run()

        StateMachineChecker.check(2)
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
        inlineMe {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    return "OK"
}
