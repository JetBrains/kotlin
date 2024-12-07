// WITH_COROUTINES
// WITH_STDLIB
// FILE: test.kt
package test

inline suspend fun f(crossinline x: suspend () -> Unit) =
    object { suspend fun foo() = x() }.foo()

// FILE: main.kt
import test.*
import kotlin.coroutines.*
import helpers.*

inline suspend fun g(crossinline x: suspend () -> Unit) =
    f(x)

var result: String = "fail"

inline suspend fun h(crossinline x: suspend () -> String) =
    g { result = x() }

fun box(): String {
    suspend { h { "OK" } }.startCoroutine(EmptyContinuation)
    return result
}
