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

var test1 = "failed"
var test2 = "failed"

fun foo(x: String) { test1 = x }

fun String.bar() { test2 = this }

fun box(): String {
    runSuspend(::foo)
    runSuspend(String::bar)
    return if(test1 == "OK" && test2 == "OK") "OK" else "fail"
}