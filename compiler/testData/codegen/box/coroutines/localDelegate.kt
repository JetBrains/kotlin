// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

class OkDelegate {
    operator fun getValue(receiver: Any?, property: Any?): String = "OK"
}

suspend fun <T> suspendWithValue(value: T): T = suspendCoroutineOrReturn { c ->
    c.resume(value)
    COROUTINE_SUSPENDED
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
