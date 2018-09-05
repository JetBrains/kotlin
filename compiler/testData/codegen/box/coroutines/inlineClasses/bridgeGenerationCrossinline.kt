// IGNORE_BACKEND: JVM_IR
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class SuccessOrFailure<T>(val a: Any?) {
    fun getOrThrow(): T = a as T
}

abstract class SuccessOrFailureReceiver<T> {
    abstract suspend fun receive(result: SuccessOrFailure<T>)
}

inline fun <T> SuccessOrFailureReceiver(crossinline f: (SuccessOrFailure<T>) -> Unit): SuccessOrFailureReceiver<T> =
    object : SuccessOrFailureReceiver<T>() {
        override suspend fun receive(result: SuccessOrFailure<T>) {
            f(result)
        }
    }

fun test() {
    var invoked = false
    val receiver = SuccessOrFailureReceiver<String> { result ->
        val intResult = result.getOrThrow()
        invoked = true
    }

    builder  {
        receiver.receive(SuccessOrFailure("42"))
    }
    if (!invoked) {
        throw RuntimeException("Fail")
    }
}

fun box(): String {
    test()
    return "OK"
}