// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

import kotlin.coroutines.*
import helpers.*

suspend fun suspendThere(v: A): A = suspendCoroutine { x ->
    x.resume(v)
}

class A(var value: Int)

suspend operator fun A?.plus(a: A) = suspendThere(A((this?.value ?: 0) + a.value))
class B(var a: A)

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var b: B? = B(A(11))
    builder { b?.a += A(31) }
    if (b?.a?.value != 42) return "FAIL 0"
    return "OK"
}
