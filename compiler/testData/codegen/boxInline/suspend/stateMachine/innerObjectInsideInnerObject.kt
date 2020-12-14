// FILE: inlined.kt
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val o = object : SuspendRunnable {
        override suspend fun run() {
            val o1 = object: SuspendRunnable {
                override suspend fun run() {
                    c()
                    c()
                }
            }
            o1.run()
            o1.run()
        }
    }
    o.run()
    o.run()
}

// FILE: inlineSite.kt
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    builder {
        crossinlineMe {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    StateMachineChecker.check(numberOfSuspensions = 16)
    return "OK"
}
