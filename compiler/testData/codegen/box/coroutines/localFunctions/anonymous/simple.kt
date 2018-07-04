// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM, JS, NATIVE
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.experimental.*

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
