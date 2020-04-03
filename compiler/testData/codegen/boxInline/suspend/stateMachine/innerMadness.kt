// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = {
        val sr = object: SuspendRunnable {
            override suspend fun run() {
                val l : suspend () -> Unit = {
                    val sr = object: SuspendRunnable {
                        override suspend fun run() {
                            val l : suspend () -> Unit = {
                                val sr = object: SuspendRunnable {
                                    override suspend fun run() {
                                        c()
                                        c()
                                    }
                                }
                                sr.run()
                                sr.run()
                            }
                            l()
                            l()
                        }
                    }
                    sr.run()
                    sr.run()
                }
                l()
                l()
            }
        }
        sr.run()
        sr.run()
    }
    l()
    l()
}

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
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
    StateMachineChecker.check(numberOfSuspensions = 256)
    return "OK"
}
