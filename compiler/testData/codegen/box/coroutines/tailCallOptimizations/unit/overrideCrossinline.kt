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
    suspend fun callMe(a: IntArray): Unit
    suspend fun callMe(s: String): Unit
    suspend fun callMe(a: Array<String>): Unit
    suspend fun callMe(a: Int): T
}

inline fun inlineMe(crossinline c: suspend () -> Unit): Base<*> {
    return object: Base<Unit> {
        override suspend fun callMe(a: IntArray) {}

        override suspend fun callMe(a: Int): Unit {
            c()
        }

        override suspend fun callMe(s: String) {}
        override suspend fun callMe(a: Array<String>) {}
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res: Any? = null

    builder {
        res = inlineMe {
            tx { Dummy }
        }.callMe(1)
    }

    (c as? Continuation<Dummy>)?.resume(Dummy)

    return if (res != Unit) "$res" else "OK"
}
