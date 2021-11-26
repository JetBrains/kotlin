// WITH_STDLIB

import kotlin.coroutines.*

interface GenericSuspendInterface<T> {
    suspend fun execute(): T
}

interface SuspendInterface : GenericSuspendInterface<Result<String>>

var c: Continuation<Result<String>>? = null
var res = "FAIL 1"

class SuspendImpl : SuspendInterface {
    override suspend fun execute(): Result<String> = suspendCoroutine { c = it }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.exceptionOrNull()!!.message!!
    })
}

fun box(): String {
    builder {
        val impl = SuspendImpl()
        val implResult = impl.execute()
        implResult.getOrThrow()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    if (res != "OK") return res

    res = "FAIL 2"
    builder {
        val iface: SuspendInterface = SuspendImpl()
        val result = iface.execute()
        result.getOrThrow()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    return res
}
