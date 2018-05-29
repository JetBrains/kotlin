// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    class A
    suspend fun A.foo() = "OK"
    var res = "FAIL"
    builder { res = (A::foo)((::A)()) }
    return res
}
