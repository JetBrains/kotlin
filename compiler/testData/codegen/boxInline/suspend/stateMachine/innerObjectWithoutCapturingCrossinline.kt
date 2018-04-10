// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

import kotlin.coroutines.experimental.intrinsics.*

var i = 0;

suspend fun suspendHere() = suspendCoroutineOrReturn<Unit> {
    i++
    COROUTINE_SUSPENDED
}

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val o = object : SuspendRunnable {
        override suspend fun run() {
            suspendHere()
            suspendHere()
            suspendHere()
            suspendHere()
            suspendHere()
        }
    }
    o.run()
}

// FILE: inlineSite.kt

import kotlin.coroutines.experimental.*

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

fun box(): String {
    var res = "OK"
    builder {
        crossinlineMe {
            res = "FAIL 1"
        }
    }
    if (i != 1) return "FAIL 2"
    return res
}