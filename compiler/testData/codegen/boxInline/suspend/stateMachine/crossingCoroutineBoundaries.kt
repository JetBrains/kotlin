// WITH_STDLIB
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*
import kotlin.coroutines.*

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
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    StateMachineChecker.reset()
    builder {
        inlineMe {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    return "OK"
}
