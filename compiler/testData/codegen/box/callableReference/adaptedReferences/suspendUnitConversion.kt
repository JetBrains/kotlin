// WITH_COROUTINES
// WITH_STDLIB
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo(x: suspend () -> Unit) = x()

suspend fun bar(): Int = suspendCoroutineUninterceptedOrReturn<Int> {
    it.resume(1)
    COROUTINE_SUSPENDED
}

fun box(): String {
    var result = ""
    suspend {
        foo(::bar)
        result += "OK"
    }.startCoroutine(EmptyContinuation)
    return result
}
