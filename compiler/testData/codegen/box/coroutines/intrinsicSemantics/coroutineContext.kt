// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import kotlin.test.assertEquals

suspend fun suspendHere() =
    if (coroutineContext != EmptyCoroutineContext)
        "${coroutineContext} != $EmptyCoroutineContext"
    else
        "OK"

suspend fun multipleArgs(a: Any, b: Any, c: Any) =
    if (coroutineContext != EmptyCoroutineContext)
        "${coroutineContext} != $EmptyCoroutineContext"
    else
        "OK"

fun builder(c: suspend () -> String): String {
    var fromSuspension: String? = null

    val continuation = object : ContinuationAdapter<String>() {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWithException(exception: Throwable) {
            fromSuspension = "Exception: ${exception}"
        }

        override fun resume(value: String) {
            fromSuspension = value
        }
    }

    c.startCoroutine(continuation)

    return fromSuspension as String
}

fun box(): String {
    var res = builder { suspendHere() }
    if (res != "OK") {
        return "fail 1 $res"
    }
    res = builder { multipleArgs(1, 1, 1) }
    if (res != "OK") {
        return "fail 2 $res"
    }
    res = builder {
        if (coroutineContext != EmptyCoroutineContext)
            "${coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"
    }
    if (res != "OK") {
        return "fail 3 $res"
    }

    return "OK"
}
