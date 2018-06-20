// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.COROUTINE_SUSPENDED
import COROUTINES_PACKAGE.intrinsics.suspendCoroutineOrReturn

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
