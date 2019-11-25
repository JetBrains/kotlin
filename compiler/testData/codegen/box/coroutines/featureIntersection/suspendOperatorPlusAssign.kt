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

class A(var value: String) {
    operator suspend fun plusAssign(other: A) {
        value = suspendThere(A(value + other.value)).value
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun usePlusAssign(): A {
    var a = A("O")
    a += A("K")
    return a
}

fun box(): String {
    var a = A("")
    builder { a = usePlusAssign() }
    return a.value
}
