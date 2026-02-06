// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
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
            // Note that "tx { Dummy }" is actually "tx<Unit> { Dummy }", and the next unsafe cast + resume
            // would make the test incorrect. For such incorrect tests there is no guarantee of the result -
            // both "Unit" and "Dummy" are possible
            tx<Dummy> { Dummy }
        }.callMe(1)
    }

    (c as? Continuation<Dummy>)?.resume(Dummy)

    return if (res != Unit) "$res" else "OK"
}
