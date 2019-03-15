// IGNORE_BACKEND: JVM_IR
// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = { c() }
    l()
}

suspend inline fun crossinlineMe2(crossinline c: suspend () -> Unit) {
    crossinlineMe { c(); c() }
}

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

var result = "FAIL"
var i = 0
var finished = false

var proceed: () -> Unit = {}

suspend fun suspendHere() = suspendCoroutine<Unit> { c ->
    i++
    proceed = { c.resume(Unit) }
}

fun builder(c: suspend () -> Unit) {
    val continuation = object: ContinuationAdapter<Unit>() {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
            proceed = {
                result = "OK"
                finished = true
            }
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    }
    c.startCoroutine(continuation)
}

fun box(): String {
    builder {
        crossinlineMe2 {
            suspendHere()
            suspendHere()
            suspendHere()
            suspendHere()
            suspendHere()
        }
    }
    for (counter in 0 until 10) {
        if (i != counter + 1) return "Expected ${counter + 1}, got $i"
        proceed()
    }
    if (i != 10) return "FAIL $i"
    if (finished) return "resume on root continuation is called"
    proceed()
    if (!finished) return "resume on root continuation is not called"
    return result
}
