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

suspend fun A.foo() = (A::bar)(this, "OK")

suspend fun A.bar(x: String) = x

fun box(): String {
    var res = "FAIL"
    builder {
        res = A().foo()
    }
    return res
}
