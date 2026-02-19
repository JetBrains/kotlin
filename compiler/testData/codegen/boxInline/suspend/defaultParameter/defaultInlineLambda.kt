// SKIP_INLINE_CHECK_IN: bar$default
// WITH_STDLIB
// WITH_COROUTINES
// FILE: 1.kt
package test

suspend fun foo(x: String) = x
fun fooNonSuspend(x: String) = x
suspend fun fooWithDefault(x: String, unused: Int = 10) = x
fun fooNonSuspendWithDefault(x: String, unused: Int = 10) = x

inline fun bar(crossinline block: suspend (String) -> String = { foo(it) }): suspend () -> String =
    { block("OK1;") }

inline fun bar2(crossinline block: suspend (String) -> String = { fooNonSuspend(it) }): suspend () -> String =
    { block("OK2;") }

inline fun bar3(crossinline block: suspend (String) -> String = { fooWithDefault(it) }): suspend () -> String =
    { block("OK3;") }

inline fun bar4(crossinline block: suspend (String) -> String = { fooNonSuspendWithDefault(it) }): suspend () -> String =
    { block("OK4;") }

// FILE: 2.kt
import test.*
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    var result = ""
    suspend {
        result += bar()()
        result += bar2()()
        result += bar3()()
        result += bar4()()
    }.startCoroutine(EmptyContinuation)
    if (result != "OK1;OK2;OK3;OK4;") return "FAIL: $result"
    return "OK"
}
