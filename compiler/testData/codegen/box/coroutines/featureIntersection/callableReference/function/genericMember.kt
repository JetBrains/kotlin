// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A<T>(val t: T) {
    suspend fun foo(): T = t
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = (A<String>::foo)(A("OK"))
    }
    return res
}
