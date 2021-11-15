// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = arrayListOf<String>()

    builder {
        while (true) {
            if (result.size == 0) {
                break
            }
        }

        for (i in 1..2) {
            result.add(suspendHere())
        }
    }

    return result[0]
}
