// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = {
        c()
    }
    l()
}

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var i = 0;

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> {
    i++
    COROUTINE_SUSPENDED
}

fun box(): String {
    builder {
        crossinlineMe {
            val sr = object: SuspendRunnable {
                override suspend fun run() {
                    val l : suspend () -> Unit = {
                        val sr = object: SuspendRunnable {
                            override suspend fun run() {
                                val l : suspend () -> Unit = {
                                    val sr = object: SuspendRunnable {
                                        override suspend fun run() {
                                            suspendHere()
                                            suspendHere()
                                            suspendHere()
                                            suspendHere()
                                            suspendHere()
                                        }
                                    }
                                    sr.run()
                                }
                                l()
                            }
                        }
                        sr.run()
                    }
                    l()
                }
            }
            sr.run()
        }
    }
    if (i != 1) return "FAIL $i"
    return "OK"
}
