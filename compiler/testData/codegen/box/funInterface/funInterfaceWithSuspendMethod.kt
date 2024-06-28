// IGNORE_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-68849

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
