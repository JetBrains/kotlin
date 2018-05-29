// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A

suspend fun A.foo(result: String) = result

fun box(): String {
    val x = A::foo
    var res = "FAIL"
    builder { res = x(A(), "OK") }
    return res
}
