// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun builder(c: suspend () -> Unit) {
    val x = c.createCoroutine(EmptyContinuation)

    x.resume(Unit)
    x.resume(Unit)
}

fun box(): String {
    var result = ""

    try {
        builder {
            result = "OK"
        }
    } catch (e: IllegalStateException) {
        return result
    }

    return "fail: $result"
}
