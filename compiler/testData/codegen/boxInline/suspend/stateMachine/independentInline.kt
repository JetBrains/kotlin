// WITH_STDLIB
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*

interface SuspendRunnable {
    suspend fun run()
    suspend fun ownIndependentInline()
}

inline fun inlineMe(crossinline c1: suspend () -> Unit) =
    object : SuspendRunnable {
        override suspend fun run() {
            c1()
            c1()
        }

        override suspend fun ownIndependentInline() {
            inlineMe2 {
                StateMachineChecker.suspendHere()
                StateMachineChecker.suspendHere()
            }.ownIndependentInline()
        }
    }


inline fun inlineMe2(crossinline c2: suspend () -> Unit): SuspendRunnable =
    object : SuspendRunnable {
        override suspend fun run() {
        }

        override suspend fun ownIndependentInline() {
            c2()
            c2()
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
    val r = inlineMe {
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
    }

    builder {
        r.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        r.ownIndependentInline()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    return "OK"
}
