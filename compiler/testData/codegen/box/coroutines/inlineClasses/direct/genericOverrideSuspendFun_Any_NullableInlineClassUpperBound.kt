// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

inline class IC(val s: Any)

interface Base<T : IC?> {
    suspend fun generic(): T
}

class Derived : Base<IC> {
    override suspend fun generic(): IC = suspendCoroutine { it.resume(IC("OK")) }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res: String? = null
    builder {
        val base: Base<*> = Derived()
        res = base.generic()!!.s as String
    }
    return res!!
}