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
    class Local {
        suspend fun foo() = "OK"
    }

    val ref = Local::foo
    var res = "FAIL"
    builder { res = ref(Local()) }
    return res
}
