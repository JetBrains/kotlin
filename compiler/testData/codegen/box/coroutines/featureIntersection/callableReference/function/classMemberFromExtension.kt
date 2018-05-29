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
    suspend fun o() = 111
    suspend fun k(k: Int) = k
}

suspend fun A.foo() = (A::o)(this) + (A::k)(this, 222)

fun box(): String {
    var result = 0
    builder { result = A().foo() }
    if (result != 333) return "Fail $result"
    return "OK"
}
