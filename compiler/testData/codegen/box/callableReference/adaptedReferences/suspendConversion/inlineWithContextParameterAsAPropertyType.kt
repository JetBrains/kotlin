// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

context(ctx: T)
fun <T> implicit(): T = ctx

var test1 = "failed"
var test2 = "failed"
var test3 = "failed"

val a: String.() -> Unit = { test1 = this }
val b: (String) -> Unit = { a: String -> test2 = a }
val c: context(String)() -> Unit = { test3 = implicit<String>() }

suspend inline fun invokeSuspend(fn: suspend context(String)() -> Unit) { fn("OK") }

fun box(): String {
    runSuspend {
        invokeSuspend(::a.get())
        invokeSuspend(::b.get())
        invokeSuspend(::c.get())
    }
    return if(test1 == "OK" && test2 == "OK" && test3 == "OK") "OK" else "fail"
}