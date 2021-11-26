// SKIP_INLINE_CHECK_IN: bar$default
// WITH_STDLIB
// WITH_COROUTINES
// FILE: 1.kt
package test

suspend fun foo(x: String) = x

inline fun bar(crossinline block: suspend (String) -> String = ::foo): suspend () -> String =
    { block("OK") }

// FILE: 2.kt
import test.*
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    var result = "fail"
    suspend {
        result = bar()()
    }.startCoroutine(EmptyContinuation)
    return result
}
