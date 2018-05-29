// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    var result = "Fail"

    suspend fun foo() {
        result = "OK"
    }
}

fun box(): String {
    val a = A()
    val x = A::foo
    builder { x(a) }
    return a.result
}
