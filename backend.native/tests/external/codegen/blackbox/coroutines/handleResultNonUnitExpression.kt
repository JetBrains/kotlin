// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*


suspend fun suspendHere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
    x.resume("OK")
    CoroutineIntrinsics.SUSPENDED
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
