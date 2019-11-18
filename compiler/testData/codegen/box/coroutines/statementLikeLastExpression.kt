// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var globalResult = ""
suspend fun suspendWithValue(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
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
