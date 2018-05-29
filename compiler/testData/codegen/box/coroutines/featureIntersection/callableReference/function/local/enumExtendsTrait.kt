// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface Named {
    suspend fun name() = "OK"
}

enum class E : Named {
    OK
}

fun box(): String {
    var res = "FAIL"
    builder { res = E.OK.name }
    return res
}
