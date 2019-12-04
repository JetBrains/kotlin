// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FULL_JDK
// WITH_RUNTIME
// WITH_REFLECT
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.reflect.full.callSuspend
import kotlin.reflect.KCallable

var c: Continuation<*>? = null

suspend fun <T> tx(lambda: () -> T): T = suspendCoroutine { c = it; lambda() }

object Dummy

suspend fun suspect() {
    tx { Dummy }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res: Any? = null
    builder {
        res = (::suspect as KCallable<*>).callSuspend()
    }

    (c as? Continuation<Dummy>)?.resume(Dummy)

    return if (res != Unit) "$res" else "OK"
}
