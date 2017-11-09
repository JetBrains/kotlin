// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendWithValue(result: () -> String): String = suspendCoroutineOrReturn { x ->
    x.resume(result())
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        fun ok() = "OK"
        result = suspendWithValue(::ok)
    }

    return result
}
