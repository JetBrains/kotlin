// WITH_STDLIB
// WITH_COROUTINES
import helpers.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendThere(v: A): A = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

class A(val value: String) {
    operator suspend fun plus(other: A) = suspendThere(A(value + other.value))
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}


fun box(): String {
    var a = A("O")

    builder {
        a += A("K")
    }

    return a.value
}
