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

interface Foo {
    suspend fun generic(): Any
}

interface Base {
    suspend fun generic(): Unit
}

class Derived: Base, Foo {
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
        val foo: Foo = Derived()
        res = foo.generic()
    }

    (c as? Continuation<Dummy>)?.resume(Dummy)

    return if (res != Unit) "$res" else "OK"
}
