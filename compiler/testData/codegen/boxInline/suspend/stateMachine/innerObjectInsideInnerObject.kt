// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val o = object : SuspendRunnable {
        override suspend fun run() {
            val o1 = object: SuspendRunnable {
                override suspend fun run() {
                    c()
                }
            }
            o1.run()
        }
    }
    o.run()
}

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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