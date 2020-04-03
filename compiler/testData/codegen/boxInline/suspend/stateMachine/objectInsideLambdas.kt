// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

interface SuspendRunnable {
    suspend fun run()
}

inline fun inlineMe(crossinline c: suspend () -> Unit) = {
    {
        val sr: SuspendRunnable = object : SuspendRunnable {
            override suspend fun run() {
                c()
                c()
            }
        }
        sr
    }()
}()

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
        }.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    return "OK"
}
