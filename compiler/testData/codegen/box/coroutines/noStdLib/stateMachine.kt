// IGNORE_BACKEND: JVM, JVM_IR
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var result = "FAIL"
var i = 0
var finished = false

var proceed: () -> Unit = {}

suspend fun suspendHere() = suspendCoroutine<Unit> {c ->
    i++
    proceed = { c.resume(Unit) }
}

suspend fun callLocal() {
    suspend fun local() {
        suspendHere()
        suspendHere()
        suspendHere()
        suspendHere()
        suspendHere()
    }
    local()
    local()
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
        callLocal()
    }
    var _counter = 0
    while (_counter < 10) {
        val counter = _counter++
        if (i != counter + 1) return "Expected ${counter + 1}, got $i"
        proceed()
    }
    if (i != 10) return "FAIL $i"
    if (finished) return "resume on root continuation is called"
    proceed()
    if (!finished) return "resume on root continuation is not called"
    return result
}
