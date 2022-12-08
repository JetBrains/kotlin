// SKIP_INLINE_CHECK_IN: bar$default
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM, ANDROID
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_IR, JVM_MULTI_MODULE_OLD_AGAINST_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
// IGNORE_INLINER: BYTECODE

// FILE: 1.kt
package test

inline fun bar(crossinline block: suspend (String) -> String = { it }): suspend () -> String =
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
