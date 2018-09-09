// IGNORE_BACKEND: JVM_IR
// FILE: inlined.kt
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var result = "FAIL"
var i = 0
var finished = false

var proceed: () -> Unit = {}

suspend fun suspendHere() = suspendCoroutine<Unit> {
    i++
    proceed = { it.resume(Unit) }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline suspend fun inlineMe() {
    suspendHere()
    suspendHere()
    suspendHere()
    suspendHere()
    suspendHere()
}

// FILE: inlineSite.kt

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    val continuation = object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(r: Result<Unit>) {
            r.getOrThrow()
            proceed = {
                result = "OK"
                finished = true
            }
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
