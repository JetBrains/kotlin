// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FULL_JDK
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

var c: Continuation<*>? = null

suspend fun <T> tx(lambda: () -> T): T = suspendCoroutine { c = it; lambda() }

object Dummy

interface Base<T> {
    suspend fun generic(): T
}

open class Derived1: Base<Unit> {
    override suspend fun generic() {}
}

class Derived2: Derived1() {
    override suspend fun generic(): Unit {
        tx { Dummy }
    }
}
fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res: Any? = null

    builder {
        val base: Base<*> = Derived2()
        res = base.generic()
    }

    (c as? Continuation<Dummy>)?.resume(Dummy)

    return if (res != Unit) "$res" else "OK"
}
