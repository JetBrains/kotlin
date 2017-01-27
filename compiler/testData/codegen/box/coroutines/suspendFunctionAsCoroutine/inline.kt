// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend inline fun suspendHere(crossinline block: () -> String): String {
    return suspendThere(block()) + suspendThere(block())
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        var q = "O"
        result = suspendHere {
            val r = q
            q = "K"
            r
        }
    }

    return result
}
