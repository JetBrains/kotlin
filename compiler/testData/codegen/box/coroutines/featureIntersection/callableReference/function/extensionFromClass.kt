// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    suspend fun result() = (A::foo)(this, "OK")
}

suspend fun A.foo(x: String) = x

fun box(): String {
    var res = "FAIL"
    builder { res = A().result() }
    return res
}
