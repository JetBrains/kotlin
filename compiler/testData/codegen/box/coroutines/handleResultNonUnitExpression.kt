// WITH_RUNTIME
// WITH_COROUTINES

suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
    x.resume("OK")
    SUSPENDED
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
