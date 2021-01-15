// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// WITH_COROUTINES
// WITH_RUNTIME
// MODULE: lib(support)
// FILE: lib.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend inline fun <R> inlined(
    crossinline step: suspend () -> R
): R = notInlined { step() }

suspend fun <R> notInlined(
    block: suspend () -> R
): R = block()

// MODULE: main(lib, support)
// WITH_COROUTINES
// WITH_RUNTIME
// FILE: main.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
