// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Int): Int {
    var res = 0

    c.createCoroutine(object : Continuation<Int> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(data: Result<Int>) {
            res = data.getOrThrow()
        }
    }).resume(Unit)

    return res
}



fun box(): String {
    var result = ""

    val handledResult = builder {
        result = suspendHere()
        56
    }

    if (handledResult != 56) return "fail 1: $handledResult"

    return result
}
