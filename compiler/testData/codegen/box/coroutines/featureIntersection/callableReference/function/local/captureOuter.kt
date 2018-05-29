// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class Outer {
    val result = "OK"

    inner class Inner {
        suspend fun foo() = result
    }
}

fun box(): String {
    val f = Outer.Inner::foo
    var res = "FAIL"
    builder { res = f(Outer().Inner()) }
    return res
}
