// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

var globalResult = ""
suspend fun suspendWithValue(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
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
