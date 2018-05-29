// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

object Singleton {
    suspend fun ok() = "OK"
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = (Singleton::ok)()
    }
    return res
}