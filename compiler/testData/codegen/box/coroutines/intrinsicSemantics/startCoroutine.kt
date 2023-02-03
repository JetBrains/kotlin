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

fun builder(c: suspend () -> String): String {
    var fromSuspension: String? = null

    c.startCoroutine(object: Continuation<String> {
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

    return fromSuspension as String
}

fun box(): String {
    if (builder { "OK" } != "OK") return "fail 4"
    if (builder { suspendHere() } != "OK") return "fail 5"
    if (builder {
            suspend {}()
            suspendHere()
    } != "OK") return "fail 51"

    if (builder { throw RuntimeException("OK") } != "Exception: OK") return "fail 6"
    if (builder { suspendWithException() } != "Exception: OK") return "fail 7"
    if (builder {
            suspend {}()
            suspendWithException()
    } != "Exception: OK") return "fail 71"

    if (builder(::suspendHere) != "OK") return "fail 8"
    if (builder(::suspendWithException) != "Exception: OK") return "fail 9"

    return "OK"
}
