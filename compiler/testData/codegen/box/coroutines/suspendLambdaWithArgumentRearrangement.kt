// IGNORE_BACKEND: JS
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
