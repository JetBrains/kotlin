// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

var resumeCoroutine: (() -> Unit)? = null

suspend fun suspendWithIncrement(value: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
    resumeCoroutine = {
        x.resume(value + 1)
    }
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0
    var acc = 0

    builder {
        for (i in 1..50) {
            acc = suspendWithIncrement(acc)
        }
        result = acc
    }

    for (i in 1..50) {
        resumeCoroutine!!.invoke()
        if (acc != i) return "Failed: expected $i, got $acc"
    }
    
    if (result != 50) return "Failed: expected 50, got $result"
    return "OK"
}
