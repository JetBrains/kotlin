// TARGET_BACKEND: JVM
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    suspend fun foo(): Unit {}
    builder {
        assert(Unit.javaClass.equals(foo().javaClass))
        assert(Unit.javaClass.equals(foo()::class.java))
    }
    return "OK"
}
