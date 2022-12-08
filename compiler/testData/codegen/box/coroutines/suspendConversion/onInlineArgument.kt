// !LANGUAGE: +SuspendConversion
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// IGNORE_INLINER: IR

// `lambda` should not be wrapped in yet another object (so no OnInlineArgumentKt$box$1$1).
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

inline suspend fun runS(fn: suspend (String) -> String) = fn("O")

fun box(): String {
    var test = "Failed"
    val lambda: (String) -> String = { it + "K" }
    suspend { test = runS(lambda) }.startCoroutine(EmptyContinuation)
    return test
}