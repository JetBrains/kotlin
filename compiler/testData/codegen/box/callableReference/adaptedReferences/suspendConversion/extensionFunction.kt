// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend Int.() -> Unit) {
    c.startCoroutine(1, EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"

fun foo(a: Int) { test1 = "O" }
fun Int.bar() { test2 = "K" }

fun box(): String {
    runSuspend(::foo)
    runSuspend(Int::bar)
    return test1+test2
}