// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class A(val v: String) {
    suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
        x.resume(v)
        SUSPENDED_MARKER
    }

    suspend fun suspendHere(): String = suspendThere("O") + suspendThere(v)
}

fun builder(c: suspend A.() -> Unit) {
    c.startCoroutine(A("K"), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
