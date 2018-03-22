// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

inline suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend inline fun complexSuspend(crossinline c: suspend () -> String): String {
    return run {
        c()
    }
}

// FILE: inleneSite.kt
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

suspend fun suspendHere(): String = suspendThere("O")

fun box(): String {
    var result = ""

    builder {
        result = suspendHere() + complexSuspend { suspendThere("K") }
    }

    return result
}
