// ISSUE: KT-72464

// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
suspend inline fun foo(f: suspend () -> String) = f()

suspend inline fun bar(f: () -> String) = foo(f)

// FILE: main.kt
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun test(): String {
    bar { return "OK" }
    return "Fail"
}

fun box(): String {
    var r = "0"
    ::test.startCoroutine(object : Continuation<String> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<String>) {
            r = result.getOrThrow()
        }
    })
    return r
}
