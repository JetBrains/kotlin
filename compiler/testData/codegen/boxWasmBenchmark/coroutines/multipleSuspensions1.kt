// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun suspendWithIncrement(value: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(value + 1)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0
    
    builder {
        var acc = 0
        for (i in 1..50) {
            acc = suspendWithIncrement(acc)
        }
        result = acc
    }
    
    if (result != 50) return "Failed: expected 50, got $result"
    return "OK"
}
