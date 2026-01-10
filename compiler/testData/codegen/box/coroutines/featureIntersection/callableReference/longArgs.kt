// WITH_COROUTINES
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend inline fun Long.longArgs(a: Long, b: Long, c: Long) = suspendCoroutineUninterceptedOrReturn<Long> {
    it.resume(this + a + b + c)
    COROUTINE_SUSPENDED
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

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
