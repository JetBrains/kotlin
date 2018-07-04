// IGNORE_BACKEND: JS, JS_IR
// COMMON_COROUTINES_TEST
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend inline fun Long.longArgs(a: Long, b: Long, c: Long) = suspendCoroutineUninterceptedOrReturn<Long> {
    it.resume(this + a + b + c)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = 0L
    val ref = 1L::longArgs
    builder {
        res = ref(1L, 1L, 1L)
    }
    return if (res == 4L) "OK" else "FAIL $res"
}
