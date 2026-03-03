// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

fun f(a: Int): Int {
    if (a <= 1) {
        return a
    }
    return f(a - 1) + f(a - 2)
}

suspend fun suspendWithIncrement(value: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(value + f(value % 20 + 10))
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0

    builder {
        var acc = 0
        for (i in 1..100) {
            acc = suspendWithIncrement(acc)
        }
        result = acc
    }

    if (result != 999) return "Failed: expected 50, got $result"
    return "OK"
}
