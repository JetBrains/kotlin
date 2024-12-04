// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.test.assertEquals

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

suspend fun suspendWithException(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resumeWithException(RuntimeException("OK"))
    COROUTINE_SUSPENDED
}

fun builder(shouldSuspend: Boolean, c: suspend () -> String): String {
    var fromSuspension: String? = null

    val result = try {
        c.startCoroutineUninterceptedOrReturn(object: Continuation<String> {
            override val context: CoroutineContext
                get() =  EmptyCoroutineContext

            override fun resumeWith(value: Result<String>) {
                fromSuspension = try {
                    value.getOrThrow()
                } catch (exception: Throwable) {
                    "Exception: " + exception.message!!
                }
            }
        })
    } catch (e: Exception) {
        "Exception: ${e.message}"
    }

    if (shouldSuspend) {
        if (result !== COROUTINE_SUSPENDED) throw RuntimeException("fail 1")
        if (fromSuspension == null) throw RuntimeException("fail 2")
        return fromSuspension!!
    }

    if (result === COROUTINE_SUSPENDED) throw RuntimeException("fail 3")
    return result as String
}

fun box(): String {
    if (builder(false) { "OK" } != "OK") return "fail 4"
    if (builder(true) { suspendHere() } != "OK") return "fail 5"
    if (builder(true) { suspend{}(); suspendHere() } != "OK") return "fail 51"

    if (builder(false) { throw RuntimeException("OK") } != "Exception: OK") return "fail 6"
    if (builder(true) { suspendWithException() } != "Exception: OK") return "fail 7"
    if (builder(true) { suspend{}(); suspendWithException() } != "Exception: OK") return "fail 71"

    if (builder(true, ::suspendHere) != "OK") return "fail 8"
    if (builder(true, ::suspendWithException) != "Exception: OK") return "fail 9"

    return "OK"
}
