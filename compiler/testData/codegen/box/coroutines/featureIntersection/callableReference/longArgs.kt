// !LANGUAGE: +ReleaseCoroutines
// !API_VERSION: 1.3
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JVM_IR
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
