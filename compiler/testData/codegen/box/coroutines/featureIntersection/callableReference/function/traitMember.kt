// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface A {
    suspend fun foo(): String
}

class B : A {
    override suspend fun foo() = "OK"
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = (A::foo)(B())
    }
    return res
}
