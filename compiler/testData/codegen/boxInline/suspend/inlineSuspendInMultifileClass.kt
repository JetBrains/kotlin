// TARGET_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR
// IGNORE_BACKEND: JVM
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// INHERIT_MULTIFILE_PARTS
// FILE: test.kt

// The lambda in box() attempts to store the result in a null Ref for some reason.

@file:JvmMultifileClass
@file:JvmName("XKt")

suspend fun suspendHere() {}

suspend inline fun test(c: suspend () -> String): String {
    suspendHere()
    val result = c()
    suspendHere()
    return result
}

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

fun box() : String {
    var res = "fail"
    suspend {
        res = test { "OK" }
    }.startCoroutine(EmptyContinuation)
    return res
}
