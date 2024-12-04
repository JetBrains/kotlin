// WITH_STDLIB
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
