// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.intrinsics.*

suspend fun foo() {
    DeferredCoroutine<Unit>().await()
}

class DeferredCoroutine<T> {
    suspend fun await(): T = awaitInternal() as T
}

internal suspend fun awaitInternal(): Any? = suspendCoroutineUninterceptedOrReturn { uCont ->

}

fun box(): String {
    return "OK"
}