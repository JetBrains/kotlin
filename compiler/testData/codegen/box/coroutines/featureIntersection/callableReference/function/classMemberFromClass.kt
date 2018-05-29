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
    suspend fun foo(k: Int) = k

    suspend fun result() = (A::foo)(this, 111)
}

fun box(): String {
    var result = 0
    builder { result = A().result() }
    if (result != 111) return "Fail $result"
    return "OK"
}
