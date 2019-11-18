// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun getLong(): Long = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(1234567890123L)
    COROUTINE_SUSPENDED
}

suspend fun suspendHere(r: LongRange): Long = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(r.start + r.endInclusive)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0L

    builder {
        result = suspendHere(1L..getLong())
    }

    if (result != 1234567890124L) return "fail 1: $result"

    return "OK"
}
