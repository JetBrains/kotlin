// WITH_STDLIB
// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8

import kotlin.coroutines.*

class A : BlockingDoubleChain

interface BlockingDoubleChain : BlockingBufferChain

interface BlockingBufferChain {
    suspend fun nextBuffer(): String = "OK"
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = A().nextBuffer()
    }
    return res
}
