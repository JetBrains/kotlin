// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

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
    l()
}

// FILE: inlineSite.kt

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

var i = 0;

suspend fun suspendHere() = suspendCoroutineOrReturn<Unit> {
    i++
    COROUTINE_SUSPENDED
}

fun box(): String {
    builder {
        crossinlineMe {
            suspendHere()
            suspendHere()
            suspendHere()
            suspendHere()
            suspendHere()
        }
    }
    if (i != 1) return "FAIL $i"
    return "OK"
}