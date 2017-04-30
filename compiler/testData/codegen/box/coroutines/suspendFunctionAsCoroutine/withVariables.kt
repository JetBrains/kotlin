// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend fun suspendHere(): String {
    val k = "K"
    val x = suspendThere("O")
    val y = x + suspendThere(k)

    return y
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
