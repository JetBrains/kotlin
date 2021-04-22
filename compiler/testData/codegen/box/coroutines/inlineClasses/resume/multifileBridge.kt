// TARGET_PLATFORM: JVM
// WITH_RUNTIME
// WITH_COROUTINES
// FILE: a.kt
@file:JvmMultifileClass
@file:JvmName("A")

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@Suppress("UNSUPPORTED_FEATURE")
inline class I(val x: Any?)

suspend fun <T> suspendHere(x: T): T = suspendCoroutineUninterceptedOrReturn {
    it.resume(x)
    COROUTINE_SUSPENDED
}

suspend fun f(): I = I(suspendHere("OK"))

// FILE: z.kt
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    var result = "fail"
    suspend { result = f().x as String }.startCoroutine(EmptyContinuation)
    return result
}
