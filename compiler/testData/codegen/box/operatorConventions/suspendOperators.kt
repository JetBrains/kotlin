// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: WASM

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

class A {
    var value = ""

    suspend operator fun get(x: Int) = value
    suspend operator fun set(x: Int, v: String) {
        value = v
    }

    operator suspend fun contains(y: String): Boolean = y == value
}

fun box() = runBlocking {
    val a = A()
    if ("" !in a) return@runBlocking "FAIL"
    a[1] = "OK"

    a[2]
}