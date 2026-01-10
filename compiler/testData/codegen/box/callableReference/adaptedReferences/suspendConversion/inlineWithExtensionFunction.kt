// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
suspend inline fun invokeSuspend(fn: suspend Int.() -> Unit) { fn(1) }

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"

fun foo(a: Int) { test1 = "O" }
fun Int.bar() { test2 = "K" }

fun box(): String {
    runSuspend {
        invokeSuspend(::foo)
        invokeSuspend(Int::bar)
    }
    return test1+test2
}