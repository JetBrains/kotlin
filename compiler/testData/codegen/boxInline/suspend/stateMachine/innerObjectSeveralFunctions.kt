// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

interface SuspendRunnable {
    suspend fun run1()
    suspend fun run2()
}

suspend inline fun crossinlineMe(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) {
    val o = object : SuspendRunnable {
        override suspend fun run1() {
            c1()
        }
        override suspend fun run2() {
            c2()
        }
    }
    o.run1()
    o.run2()
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
var j = 0;

suspend fun incrementI() {
    i++
}

suspend fun incrementJ() {
    j++
}

fun box(): String {
    builder {
        crossinlineMe({ incrementI() }) { incrementJ() }
    }
    if (i != 1) return "FAIL i $i"
    if (j != 1) return "FAIL i $i"
    return "OK"
}