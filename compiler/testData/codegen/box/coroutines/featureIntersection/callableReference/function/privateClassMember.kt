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
    private suspend fun foo() = "OK"

    suspend fun bar() = (A::foo)(this)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = A().bar()
    }
    return res
}
