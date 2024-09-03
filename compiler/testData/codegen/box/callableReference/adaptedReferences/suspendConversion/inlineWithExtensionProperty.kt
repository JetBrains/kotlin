// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"

val a: Int.() -> Unit = { test1 = "O" }
val b: (Int) -> Unit = { test2 = "K" }

suspend inline fun invokeSuspend(fn: suspend Int.() -> Unit) { fn(1) }

fun box(): String {
    runSuspend {
        invokeSuspend(::a.get())
        invokeSuspend(::b.get())
    }
    return test1+test2
}