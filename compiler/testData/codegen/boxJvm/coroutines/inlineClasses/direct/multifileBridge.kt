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

suspend fun <T> suspendHere(t: T): T = t

suspend fun f(): I = I(suspendHere("OK"))

// FILE: z.kt
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    var result = "fail"
    suspend { result = f().x as String }.startCoroutine(EmptyContinuation)
    return result
}
