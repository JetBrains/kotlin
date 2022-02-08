// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var stopped = false
var log = ""
var postponed: () -> Unit = { }

suspend fun delay(): Unit = suspendCoroutine { c ->
    log += "1"
    postponed = {
        log += "3"
        c.resume(Unit)
    }
    COROUTINE_SUSPENDED
}

suspend inline fun foo(x: String): String {
    delay()
    return x
}

suspend inline fun bar(x: String) = foo(x)

suspend fun baz(x: String) = bar(x)

fun act(c: suspend () -> Unit) {
    stopped = false

    c.startCoroutine(handleResultContinuation {
        stopped = true
    })

    while (!stopped) {
        log += "2"
        postponed()
    }
}

fun box(): String {
    var result = ""
    act {
        result = baz("OK")
    }

    if (log != "123") return "fail: $log"
    return result
}
