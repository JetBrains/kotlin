// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
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
        val intResult = result.getOrThrow()
        invoked = true
    }

    builder {
        receiver.receive(Result(suspendMe()))
    }
    c?.resume(42)
    if (!invoked) {
        throw RuntimeException("Fail")
    }
}

fun box(): String {
    test()
    return "OK"
}
