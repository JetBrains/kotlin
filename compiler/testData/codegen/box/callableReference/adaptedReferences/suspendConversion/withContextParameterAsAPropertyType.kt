// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend context(String) () -> Unit) {
    c.startCoroutine("OK", EmptyContinuation)
}

context(ctx: T)
fun <T> implicit(): T = ctx

var result1  = "failed"
var result2  = "failed"
var result3  = "failed"

val a: context(String)() -> Unit
    get() = { result1 = implicit<String>() }

val b: String.() -> Unit
    get() = { result2 = this }

val c: (String) -> Unit
    get() = { a: String -> result3 = a }


fun box(): String {
    runSuspend(::a.get())
    runSuspend(::b.get())
    runSuspend(::c.get())
    return if(result1 == "OK" && result2 == "OK" && result3 == "OK") "OK" else "fail"
}