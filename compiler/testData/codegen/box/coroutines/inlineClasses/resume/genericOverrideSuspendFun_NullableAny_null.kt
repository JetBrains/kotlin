// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

inline class IC(val s: Any?)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

interface Base<T> {
    suspend fun generic(): T
}

class Derived : Base<IC> {
    override suspend fun generic(): IC = suspendMe()
}

fun Any?.toResultString(): String =
    if (this == null) "OK" else "!! $this"

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res: String? = null
    builder {
        val base: Base<*> = Derived()
        res = (base.generic() as IC).s.toResultString()
    }
    c?.resume(IC(null))
    return res!!
}