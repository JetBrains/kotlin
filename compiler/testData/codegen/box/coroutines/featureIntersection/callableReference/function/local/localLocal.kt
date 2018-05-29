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
    suspend fun foo(): String {
        suspend fun bar() = "OK"
        val ref = ::bar
        return ref()
    }

    val ref = ::foo
    var res = "FAIL"
    builder { res = ref() }
    return res
}
