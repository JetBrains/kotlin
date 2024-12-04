// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun someCondition() = true

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn {
    it.resume(Unit)
    COROUTINE_SUSPENDED
}

fun expectString(x: String?) = x!!

suspend fun foo(): String {
    var x: String? = null
    if (someCondition()) {
        x = "OK"
    }
    suspendHere()
    return expectString(x)
}

fun box(): String {
    var result = "fail"
    suspend { result = foo() }.startCoroutine(EmptyContinuation)
    return result
}