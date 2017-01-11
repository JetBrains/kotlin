// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    SUSPENDED_MARKER
}

suspend fun suspendHere(suspend: Boolean): String {
    if (!suspend) return "O"

    return suspendThere("") + suspendThere("K")
}
fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere(false) + suspendHere(true)
    }

    return result
}
