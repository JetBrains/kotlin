// IGNORE_BACKEND_FIR: JVM_IR
// FILE: inlined.kt
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = {
        val l1 : suspend () -> Unit = {
            c()
            c()
        }
        l1()
        l1()
    }
    l()
    l()
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
