// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.SUSPENDED_MARKER
import kotlin.coroutines.intrinsics.suspendCoroutineOrReturn

class OkDelegate {
    operator fun getValue(receiver: Any?, property: Any?): String = "OK"
}

suspend fun <T> suspendWithValue(value: T): T = suspendCoroutineOrReturn { c ->
    c.resume(value)
    SUSPENDED_MARKER
}

fun launch(c: suspend () -> String): String {
    var result: String = "fail: result not assigned"
    c.startCoroutine(handleResultContinuation { value ->
        result = value
    })
    return result
}

fun box(): String {
    return launch {
        val ok by OkDelegate()
        ok
    }
}