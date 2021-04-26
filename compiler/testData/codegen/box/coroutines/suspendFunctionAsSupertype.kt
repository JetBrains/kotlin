// WITH_RUNTIME
// IGNORE_BACKEND: JS, JS_IR, JVM
// !LANGUAGE: +SuspendFunctionAsSupertype

import kotlin.coroutines.*

var res = "FAIL"

class C: suspend () -> Unit {
    override suspend fun invoke() {
        res = "OK"
    }
}

fun box(): String {
    val o: suspend () -> Unit = object : suspend () -> Unit {
        override suspend fun invoke() {
            res = "OK"
        }
    }
    o.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
    if (res != "OK") return "FAIL 1: $res"

    res = "FAIL 2"
    C().startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
    return res
}