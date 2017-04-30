// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Int): Int {
    var res = 0

    c.createCoroutine(object : Continuation<Int> {
        override val context = EmptyCoroutineContext

        override fun resume(data: Int) {
            res = data
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
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
