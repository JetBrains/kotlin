// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
    val continuation = object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(value: Result<Unit>) {
            value.getOrThrow()
            proceed = {
                result = "OK"
                finished = true
            }
        }
    }
    c.startCoroutine(continuation)
}

fun box(): String {
    builder {
        callLocal()
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
