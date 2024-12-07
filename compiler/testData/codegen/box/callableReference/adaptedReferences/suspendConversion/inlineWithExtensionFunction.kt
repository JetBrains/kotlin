// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"

fun foo(a: Int) { test1 = "O" }
fun Int.bar() { test2 = "K" }

suspend inline fun invokeSuspend(fn: suspend Int.() -> Unit) { fn(1) }

fun box(): String {
    runSuspend {
        invokeSuspend(::foo)
        invokeSuspend(Int::bar)
    }
    return test1+test2
}