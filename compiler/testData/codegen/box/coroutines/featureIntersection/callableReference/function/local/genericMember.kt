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
    class Id<T> {
        suspend fun invoke(t: T) = t
    }

    val ref = Id<String>::invoke
    var res = "FAIL"
    builder { res = ref(Id<String>(), "OK") }
    return res
}
