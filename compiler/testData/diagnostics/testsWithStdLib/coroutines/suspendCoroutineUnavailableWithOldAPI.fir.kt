// !API_VERSION: 1.0
// SKIP_TXT

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun foo(): Unit = suspendCoroutine {
    it.resume(Unit)
}

suspend fun bar(): Unit = suspendCoroutineUninterceptedOrReturn {
    it.resume(Unit)
    COROUTINE_SUSPENDED
}
