// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
