// IGNORE_BACKEND: JS
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

enum class E {
    A, B;

    suspend fun foo() = this.name
}

fun box(): String {
    val f = E.A::foo
    val ef = E::foo

    var res = ""
    builder {
        if (f() != "A") res = "Fail 1: ${f()}"
        else if (f == E.B::foo) res = "Fail 2"
        else if (ef != E::foo) res = "Fail 3"
    }

    return "OK"
}
