// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
// FILE: a.kt
@file:JvmMultifileClass
@file:JvmName("A")

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@Suppress("UNSUPPORTED_FEATURE")
inline class I(val x: Any?)

suspend fun <T> suspendHere(): T = suspendCoroutineUninterceptedOrReturn {
    c = it as Continuation<Any?>
    COROUTINE_SUSPENDED
}

var c: Continuation<Any?>? = null

suspend fun f(): I = I(suspendHere<String>())

// FILE: z.kt
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    var result = "fail"
    suspend { result = f().x as String }.startCoroutine(EmptyContinuation)
    c?.resume("OK")
    return result
}
