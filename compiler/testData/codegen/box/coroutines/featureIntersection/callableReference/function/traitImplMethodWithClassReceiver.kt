// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface T {
    suspend fun foo() = "OK"
}

class B : T {
    inner class C {
        suspend fun bar() = (T::foo)(this@B)
    }
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = B().C().bar()
    }
    return res
}
