// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_COROUTINES
// WITH_STDLIB

import kotlin.coroutines.*
import helpers.*

interface S {
    suspend fun foo()
}

interface T : S {
    override suspend fun foo() {
        bar()
    }

    fun bar()
}

object O : T {
    var result = ""

    override fun bar() {
        result = "OK"
    }
}

fun builder(block: suspend () -> Unit) {
    block.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder { O.foo() }
    return O.result
}
