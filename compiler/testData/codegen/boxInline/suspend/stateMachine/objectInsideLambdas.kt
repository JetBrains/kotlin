// WITH_STDLIB
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
import helpers.*
import kotlin.coroutines.*

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
