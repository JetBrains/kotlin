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
    suspend fun Int.is42With(that: Int) = this + 2 * that == 42
    var res = ""
    builder { res = if ((Int::is42With)(16, 13)) "OK" else "Fail" }
    return res
}
