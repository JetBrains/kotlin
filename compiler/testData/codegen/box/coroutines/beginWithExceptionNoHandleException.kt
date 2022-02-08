// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
suspend fun suspendHere(): Any = suspendCoroutineUninterceptedOrReturn { x ->}

fun builder(c: suspend () -> Unit) {
    try {
        c.createCoroutine(EmptyContinuation).resumeWithException(RuntimeException("OK"))
    }
    catch(e: Exception) {
        if (e?.message != "OK") {
            throw RuntimeException("Unexpected result: ${e?.message}")
        }
        return
    }

    throw RuntimeException("Exception must be thrown above")
}

fun box(): String {
    var result = "OK"
    builder {
        suspendHere()
        result = "fail 1"
    }

    builder {
        result = "fail 2"
    }

    return result
}
