// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

inline fun callAction(aux: Int, action: () -> String): String {
    return action()
}

suspend fun get() = "OK"

suspend fun callSuspend(): String {
    return callAction(action = {
        get()
    }, aux = 0)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var v = "fail"
    builder {
        v = callSuspend()
    }
    return v
}
