// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// JVM_TARGET: 1.8

import kotlin.coroutines.*

interface IOk {
    fun ok(): String = "OK"
}

inline class InlineClass(val s: String) : IOk

suspend fun returnsUnboxed() = InlineClass("")

suspend fun test(): String = returnsUnboxed().ok()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {})
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = test()
    }
    return res
}