// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    x.resumeWithException(IllegalStateException())
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class Data(val x: Int)

fun box(): String {
    var result = ""

    builder {
        val data = Data(42)
        try {
            try {
                suspendHere()
            } catch (_: NoSuchElementException) { }
        } catch (e: IllegalStateException) {
            result = if (data.x == 42) "OK" else "fail"
        }
    }

    return result
}
