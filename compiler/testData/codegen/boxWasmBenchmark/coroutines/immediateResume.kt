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

fun builder(shouldSuspend: Boolean, c: suspend () -> String): String {
    var fromSuspension: String? = null

    val result = try {
        c.startCoroutineUninterceptedOrReturn(object : Continuation<String> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

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
    if (builder(true) {
            var res = ""
            for (i in 1..30) {
                res = suspendHere()
            }
            res
        } != "OK") return "fail 5"

    return "OK"
}
