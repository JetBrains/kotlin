// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// WITH_COROUTINES

// MODULE: lib
// FILE: A.kt

package a

context(string: String)
suspend fun f() = string

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
