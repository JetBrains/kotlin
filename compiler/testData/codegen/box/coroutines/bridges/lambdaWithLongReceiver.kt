// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// CHECK_BYTECODE_LISTING

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun call(c: suspend Long.() -> String): String {
    return 1000L.c()
}

fun box(): String {
    var res = ""
    builder {
        res = call { ->
            "OK$this"
        }
    }
    if (res != "OK1000") return res
    return "OK"
}