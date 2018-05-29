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

fun box(): String {
    var result = "Fail"

    suspend fun A.ext() { result = "OK" }

    val f = A::ext
    builder { f(A()) }
    return result
}
