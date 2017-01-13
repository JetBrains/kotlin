// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var globalResult = ""
suspend fun suspendWithValue(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    SUSPENDED_MARKER
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
