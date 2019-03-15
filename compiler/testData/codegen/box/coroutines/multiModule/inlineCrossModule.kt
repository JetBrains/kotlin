// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// WITH_COROUTINES
// WITH_RUNTIME
// COMMON_COROUTINES_TEST

// MODULE: lib(support)
// FILE: lib.kt

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend inline fun <R> inlined(
    crossinline step: suspend () -> R
): R = notInlined { step() }

suspend fun <R> notInlined(
    block: suspend () -> R
): R = block()

// MODULE: main(lib, support)
// FILE: main.kt
// WITH_COROUTINES
// WITH_RUNTIME
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var result = "FAIL"

suspend fun test() {
    inlined {
        result = "OK"
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        test()
    }
    return result
}