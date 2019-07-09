// IGNORE_BACKEND: JVM_IR
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

interface SuspendRunnable {
    suspend fun run()
    suspend fun run2()
}

inline fun inlineMe(crossinline c: suspend () -> Unit, crossinline c2: suspend () -> Unit) =
    object : SuspendRunnable {
        override suspend fun run() {
            c()
            c()
        }

        override suspend fun run2() {
            c2()
            c2()
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
    val r = inlineMe(
        {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    ) {
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
    }
    builder {
        r.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        r.run2()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    return "OK"
}
