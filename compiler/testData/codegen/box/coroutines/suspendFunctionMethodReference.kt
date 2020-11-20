// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND_FIR: JVM_IR

import kotlin.coroutines.*
import helpers.*

fun f(a: suspend () -> Unit) {
    val f = a::invoke
    f.startCoroutine(EmptyContinuation())
}

fun box(): String {
    var result = ""
    f {
        result = "OK"
    }
    return result
}
