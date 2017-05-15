// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*


suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    var isCompleted = false
    c.startCoroutine(handleResultContinuation {
        isCompleted = true
    })
    if (!isCompleted) throw RuntimeException("fail")
}

fun box(): String {
    builder {
        "OK"
    }

    builder {
        suspendHere()
    }

    return "OK"
}
