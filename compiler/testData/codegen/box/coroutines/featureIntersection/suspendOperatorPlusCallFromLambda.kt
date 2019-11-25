// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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
