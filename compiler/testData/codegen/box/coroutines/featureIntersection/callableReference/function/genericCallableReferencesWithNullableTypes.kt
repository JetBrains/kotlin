// IGNORE_BACKEND: NATIVE
// WITH_REFLECT
// WITH_COROUTINES

import kotlin.test.assertEquals

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun <T, R> foo(x: T): R = TODO()

suspend inline fun <reified T, reified R> bar(x: T, y: R, f: suspend (T) -> R, tType: String, rType: String): Pair<T, R?> {
    assertEquals(tType, T::class.simpleName)
    assertEquals(rType, R::class.simpleName)
    return Pair(x, y)
}

data class Pair<A, B>(val a: A, val b: B)

fun box(): String {
    builder {
        bar(1, "", ::foo, "Int", "String")

        val s1: Pair<Long, String?> = bar(1L, "", ::foo, "Long", "String")
        val (a: Long, b: String?) = bar(1L, "", ::foo, "Long", "String")

        val ns: String? = null
        bar(ns, ns, ::foo, "String", "String")

        val s2: Pair<Int?, String?> = bar(null, null, ::foo, "Int", "String")
    }

    return "OK"
}
