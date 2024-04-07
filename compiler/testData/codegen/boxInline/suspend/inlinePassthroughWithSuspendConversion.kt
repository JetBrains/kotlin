// WITH_COROUTINES
// WITH_STDLIB
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR
// FILE: test.kt
package test

inline suspend fun f(crossinline x: suspend () -> Unit) =
    object { suspend fun foo() = x() }.foo()

// FILE: main.kt
import test.*
import kotlin.coroutines.*
import helpers.*

inline suspend fun g(crossinline x: () -> Unit) =
    f(x)

var result: String = "fail"

inline suspend fun h(crossinline x: () -> String) =
    g { result = x() }

fun box(): String {
    suspend { h { "OK" } }.startCoroutine(EmptyContinuation)
    return result
}