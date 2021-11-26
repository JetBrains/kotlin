// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

inline class IC(val s: String)

suspend fun foo(x: String = "OK") = suspendCoroutineUninterceptedOrReturn<IC> {
    it.resume(IC(x))
    COROUTINE_SUSPENDED
}

fun box(): String {
    var res = ""
    suspend { res = foo().s }.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return res
}
