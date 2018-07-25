// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendWithValue(result: () -> String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(result())
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        fun ok() = "OK"
        result = suspendWithValue(::ok)
    }

    return result
}
