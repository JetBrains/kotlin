// WITH_STDLIB
// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8

import kotlin.coroutines.*

interface Foo {
    private suspend fun test(): String {
        return "OK"
    }

    suspend fun foo(): String {
        return test()
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var res = "FAIL"

    builder {
        val foo: Foo = object : Foo{}
        res = foo.foo()
    }

    return res
}