// WITH_STDLIB
// WITH_COROUTINES
// FILE: lib.kt
import kotlin.coroutines.*

@Suppress("UNSUPPORTED_FEATURE")
inline class Result<T>(val a: Any?) {
    fun getOrThrow(): T = a as T
}

abstract class ResultReceiver<T> {
    abstract suspend fun receive(result: Result<T>)
}

inline fun <T> ResultReceiver(crossinline f: (Result<T>) -> Unit): ResultReceiver<T> =
    object : ResultReceiver<T>() {
        override suspend fun receive(result: Result<T>) {
            f(result)
        }
    }

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

fun test() {
    var invoked = false
    val receiver = ResultReceiver<String> { result ->
        val intResult = result.getOrThrow()
        invoked = true
    }

    builder  {
        receiver.receive(Result(suspendMe()))
    }
    c?.resume("42")
    if (!invoked) {
        throw RuntimeException("Fail")
    }
}

fun box(): String {
    test()
    return "OK"
}
