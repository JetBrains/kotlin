// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun someCondition() = false

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn {
    it.resume(Unit)
    COROUTINE_SUSPENDED
}

fun expectString(x: String?) = x!!

suspend fun foo(): String {
    var x: String? = "OK"
    if (someCondition()) {
        x = null as String?
    }
    suspendHere()
    return expectString(x)
}

fun box(): String {
    var result = "fail"
    suspend { result = foo() }.startCoroutine(EmptyContinuation)
    return result
}
