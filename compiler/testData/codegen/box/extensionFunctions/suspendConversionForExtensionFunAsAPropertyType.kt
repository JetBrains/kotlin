// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend Int.() -> Unit) {
    c.startCoroutine(1, EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"

val a: Int.() -> Unit = { test1 = "O" }
val b: (Int) -> Unit = { test2 = "K" }

fun box(): String {
    runSuspend(a)
    runSuspend(b)
    return test1+test2
}