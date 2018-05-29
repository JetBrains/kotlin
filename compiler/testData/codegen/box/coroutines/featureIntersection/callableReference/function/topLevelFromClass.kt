// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo(o: Int, k: Int) = o + k

class A {
    suspend fun bar() = (::foo)(111, 222)
}

fun box(): String {
    var result = 0
    builder { result = A().bar() }
    if (result != 333) return "Fail $result"
    return "OK"
}
