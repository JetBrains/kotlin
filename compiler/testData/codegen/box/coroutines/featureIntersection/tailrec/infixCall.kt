// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// DONT_RUN_GENERATED_CODE: JS

import helpers.*
import COROUTINES_PACKAGE.*

tailrec suspend infix fun Int.test(x : Int) : Int {
    if (this > 1) {
        return (this - 1) test x
    }
    return this
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var res = ""
    builder {
        res = if (1000000.test(1000000) == 1) "OK" else "FAIL"
    }
    return res
}