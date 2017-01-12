// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

suspend fun suspendHere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
    x.resume("OK")
    CoroutineIntrinsics.SUSPENDED
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
