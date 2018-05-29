// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var result = "Fail"

suspend fun foo(newResult: String) {
    result = newResult
}

fun box(): String {
    val x = ::foo
    builder { x("OK") }
    return result
}
