// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Int): Int {
    var res = 0
    c.startCoroutine(handleResultContinuation {
        res = it
    })

    return res
}

fun box(): String {
    var result = ""

    val handledResult = builder {
        result = suspendHere()
        return@builder 56
    }

    if (handledResult != 56) return "fail 1: $handledResult"

    return result
}
