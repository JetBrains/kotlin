// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class Data(val x: Int)

fun box(): String {
    var result = ""

    builder {
        var data = Data(0)
        var done = false
        do {
            data = Data(1)
            suspendHere()
            done = true
        } while (!done)
        result = if (data.x == 1) "OK" else "fail"
    }

    return result
}
