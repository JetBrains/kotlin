// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

var result = "FAIL"
var i = 0
var finished = false

var proceed: () -> Unit = {}

suspend fun suspendHere() = suspendCoroutine<Unit> {
    i++
    proceed = { it.resume(Unit) }
}

inline suspend fun inlineMe() {
    suspendHere()
    suspendHere()
    suspendHere()
    suspendHere()
    suspendHere()
}

// FILE: inlineSite.kt

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    val continuation = object: Continuation<Unit> {
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

suspend fun inlineSite() {
    inlineMe()
    inlineMe()
}

fun box(): String {
    builder {
        inlineSite()
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