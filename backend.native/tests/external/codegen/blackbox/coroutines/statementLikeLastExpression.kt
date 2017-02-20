// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

var globalResult = ""
suspend fun suspendWithValue(v: String): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
    x.resume(v)
    CoroutineIntrinsics.SUSPENDED
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
