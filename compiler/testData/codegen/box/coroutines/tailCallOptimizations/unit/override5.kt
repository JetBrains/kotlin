// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// FIR status: questionable bytecode listing difference (open/final methods in final class)
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

import helpers.*
import kotlin.coroutines.*

var c: Continuation<*>? = null

interface Base<T> {
    suspend fun generic(): T
}

inline fun inlineMe(crossinline c: suspend () -> Unit) = object : Base<Unit> {
    override suspend fun generic(): Unit {
        c();
        {}.let { it() }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        inlineMe { }.generic()
    }

    return "OK"
}
