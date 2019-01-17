// IGNORE_BACKEND: JVM_IR
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_BYTECODE_LISTING

import helpers.*
import COROUTINES_PACKAGE.*

suspend fun dummy(): Unit = Unit
suspend fun test(): Int = 1.also {
    dummy()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = 0
    builder {
        res = test()
    }
    return if (res == 1) "OK" else "FAIL"
}
