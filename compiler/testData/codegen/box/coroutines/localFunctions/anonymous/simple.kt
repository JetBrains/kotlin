// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR, JS_IR
// IGNORE_BACKEND: JVM, JS, NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun callLocal(): String {
    val local = suspend fun() = "OK"
    return local()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = callLocal()
    }
    return res
}
