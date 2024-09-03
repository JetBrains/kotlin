// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend Int.() -> Unit) {
    c.startCoroutine(1, EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"

fun foo(): Int.() -> Unit = { test1 = "O" }
fun bar(): (Int) -> Unit = { test2 = "K" }

fun box(): String {
    runSuspend(foo())
    runSuspend(bar())
    return test1+test2
}