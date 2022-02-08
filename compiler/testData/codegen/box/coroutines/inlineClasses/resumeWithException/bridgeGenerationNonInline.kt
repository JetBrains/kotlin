// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

var result = "FAIL"

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
}

@Suppress("UNSUPPORTED_FEATURE")
inline class Result<T>(val a: Any?) {
    fun getOrThrow(): T = a as T
}

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

abstract class ResultReceiver<T> {
    abstract suspend fun receive(result: Result<T>)
}

fun <T> ResultReceiver(f: (Result<T>) -> Unit): ResultReceiver<T> =
    object : ResultReceiver<T>() {
        override suspend fun receive(result: Result<T>) {
            f(result)
        }
    }

fun test() {
    var invoked = false
    val receiver = ResultReceiver<Int> { result ->
        invoked = true
        result.getOrThrow()
    }

    builder {
        receiver.receive(Result(suspendMe()))
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (invoked) {
        throw RuntimeException("Fail")
    }
}

fun box(): String {
    test()
    return result
}
