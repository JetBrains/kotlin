// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo(x: Any): Int {
    return if (x == "56") suspendHere() else 13
}

suspend fun suspendHere(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(56)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = -1

    builder {
        result = foo("56")
    }

    if (result != 56) return "fail 1: $result"

    return "OK"
}
