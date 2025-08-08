// WITH_STDLIB
// ISSUE: KT-68849

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0
// ^^^ KT-68849 fixed in 2.0.20-Beta2

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun interface SuspendFun {
    suspend fun method(): String
}

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

fun box(): String {
    val impl: () -> String = { "OK" }
    val suspendImpl = SuspendFun(impl)
    return runBlocking {
        suspendImpl.method()
    }
}
