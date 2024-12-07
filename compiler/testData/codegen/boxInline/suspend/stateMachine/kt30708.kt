// WITH_STDLIB
// WITH_COROUTINES
// CHECK_STATE_MACHINE
// FILE: inline.kt

import helpers.*

interface SuspendRunnable {
    suspend fun run(): String
}

inline fun inlineMe(crossinline c1: suspend (String) -> String) =
    object : SuspendRunnable {
        override suspend fun run(): String {
            return c1(
                return try { "OK" } catch (e: Exception) { e.message!! }
            )
        }
    }


inline fun inlineMe2(crossinline c2: suspend (String) -> String) =
    inlineMe(c2)

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
        it
    }

    var res = "FAIL"

    builder {
        res = r.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 0)
    return res
}
