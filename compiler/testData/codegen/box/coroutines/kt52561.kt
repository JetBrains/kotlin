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

fun expectString(x: String?) {
    if (x != "TEST") error("FAIL: $x")
}

suspend fun test(block: (String) -> Unit) {
    val string: String? = "TEST".takeIf { true }
    check(string != null) // smartcast from String? to String
    expectString(string)
    suspendHere()
    expectString(string)
    block(string)
}


fun box(): String {
    suspend { test {} }.startCoroutine(EmptyContinuation)
    return "OK"
}