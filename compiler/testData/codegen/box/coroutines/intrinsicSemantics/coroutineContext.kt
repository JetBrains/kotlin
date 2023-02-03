// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
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

    val continuation = object : Continuation<String> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(value: Result<String>) {
            fromSuspension = try { value.getOrThrow() } catch (exception: Throwable) { "Exception: ${exception}" }
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
    res = builder(::suspendHere)
    if (res != "OK") {
        return "fail 4 $res"
    }
    res = builder {
        suspend {}()
        suspendHere()
    }
    if (res != "OK") {
        return "fail 5 $res"
    }

    return "OK"
}
