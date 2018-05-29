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
    val result = "OK"

    class Local {
        suspend fun foo() = result
    }

    val member = Local::foo
    val instance = Local()
    var res = "FAIL"
    builder { res = member(instance) }
    return res
}
