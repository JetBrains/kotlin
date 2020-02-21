// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

object Dummy

suspend fun suspect(): Dummy = suspendCoroutineUninterceptedOrReturn {
    x -> x.resume(Dummy)
    COROUTINE_SUSPENDED
}

fun box(): String {
    var res: Any? = null
    suspend {
        res = (::suspect as suspend () -> Unit)()
    }.startCoroutine(EmptyContinuation)
    return if (res != Unit) "$res" else "OK"
}
