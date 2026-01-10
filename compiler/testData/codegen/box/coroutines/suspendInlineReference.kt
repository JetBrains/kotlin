// WITH_STDLIB
// WITH_COROUTINES
// FILE: lib.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn {
    it.resume(Unit)
    COROUTINE_SUSPENDED
}

suspend inline fun foo(): String {
    suspendHere()
    return "OK"
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    var result = ""
    suspend {
        val ref = ::foo
        result = ref()
    }.startCoroutine(EmptyContinuation)
    return result
}
