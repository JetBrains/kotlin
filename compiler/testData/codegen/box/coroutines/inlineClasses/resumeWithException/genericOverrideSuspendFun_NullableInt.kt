// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

var result = "FAIL"

inline class IC(val s: Int?)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

fun Int?.toResultString() =
    if (this == 42) "OK" else "!! $this"

interface Base<T> {
    suspend fun generic(): T
}

class Derived : Base<IC> {
    override suspend fun generic(): IC = suspendMe()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
}

fun box(): String {
    builder {
        val base: Base<*> = Derived()
        (base.generic() as IC).s.toResultString()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    return result
}