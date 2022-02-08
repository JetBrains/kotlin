// WITH_STDLIB

import kotlin.coroutines.*

var res: String = "FAIL"

class Log {
    fun error(message: Any?) {
        res = message as String
    }
}

private val log = Log()

class C {
    fun method() {}
}

fun <T : Any> df(t: T, r: suspend (T) -> Unit) {
    r.startCoroutine(t, Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun foo(s: String, c: C?) {
    df(s) {
        c?.method() ?: log.error(it)
    }
}

fun box(): String {
    foo("OK", null)
    return res
}