// IGNORE_BACKEND: JVM_IR
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class Result<T>(val a: Any?) {
    fun getOrThrow(): T = a as T
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
        receiver.receive(Result(42))
    }
    if (!invoked) {
        throw RuntimeException("Fail")
    }
}

fun box(): String {
    test()
    return "OK"
}