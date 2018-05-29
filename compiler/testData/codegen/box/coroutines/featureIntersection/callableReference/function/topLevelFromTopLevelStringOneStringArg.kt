// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo(x: String) = x

fun box(): String {
    val x = ::foo
    var res = "FAIL"
    builder { res = x("OK") }
    return res
}
