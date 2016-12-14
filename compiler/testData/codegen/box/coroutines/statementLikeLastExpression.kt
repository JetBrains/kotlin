// WITH_RUNTIME
// WITH_COROUTINES
var globalResult = ""
suspend fun suspendWithValue(v: String): String = suspendWithCurrentContinuation { x ->
    x.resume(v)
    SUSPENDED
}

fun builder(c: suspend () -> String) {
    c.startCoroutine(handleResultContinuation {
        globalResult = it
    })
}

fun box(): String {

    var condition = true

    builder {
        if (condition) {
            suspendWithValue("OK")
        } else {
            suspendWithValue("fail 1")
        }
    }

    return globalResult
}
