// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES

// MODULE: lib
// FILE: A.kt

package a

context(String)
suspend fun f() = this@String

// MODULE: main(lib)
// FILE: B.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    var result: String = "fail"
    val block: suspend () -> String = {
        with("OK") { a.f() }
    }
    block.startCoroutine(handleResultContinuation { value ->
        result = value
    })
    return result
}
