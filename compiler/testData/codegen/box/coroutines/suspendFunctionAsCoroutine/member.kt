// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class A(val v: String) {
    suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
        x.resume(v)
        COROUTINE_SUSPENDED
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
