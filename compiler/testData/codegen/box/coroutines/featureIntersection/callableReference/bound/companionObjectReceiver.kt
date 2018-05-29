// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

class A {
    companion object {
        suspend fun ok() = "OK"
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = (A.Companion::ok)()
    }
    return res
}