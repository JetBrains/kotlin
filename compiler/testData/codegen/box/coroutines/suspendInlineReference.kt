// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM, JS
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

fun box(): String {
    var result = ""
    suspend {
        val ref = ::foo
        result = ref()
    }.startCoroutine(EmptyContinuation)
    return result
}
