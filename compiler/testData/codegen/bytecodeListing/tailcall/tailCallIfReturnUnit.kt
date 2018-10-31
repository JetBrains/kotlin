// WITH_RUNTIME

import kotlin.coroutines.intrinsics.*

fun check() = true

suspend fun f(i: Int): Unit {
    return f_2()
}

private inline suspend fun f_2(): Unit {
    if (check()) return
    return suspendCoroutineUninterceptedOrReturn {
        COROUTINE_SUSPENDED
    }
}
