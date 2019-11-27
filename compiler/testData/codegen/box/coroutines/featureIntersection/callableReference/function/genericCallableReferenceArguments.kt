// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES

import kotlin.test.assertEquals

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun <T, R> foo(x: T): R = TODO()
suspend fun <T> fooReturnLong(x: T): Long = 1L
suspend fun Int.suspendToString(): String = toString()

suspend inline fun <reified T, reified R> check(x: T, y: R, f: suspend (T) -> R, tType: String, rType: String) {
    assertEquals(tType, T::class.simpleName)
    assertEquals(rType, R::class.simpleName)
}

suspend inline fun <reified T, reified R> check(f: suspend (T) -> R, g: suspend (T) -> R, tType: String, rType: String) {
    assertEquals(tType, T::class.simpleName)
    assertEquals(rType, R::class.simpleName)
}

fun box(): String {
    builder {
        check("", 1, ::foo, "String", "Int")
        check("", 1L, ::fooReturnLong, "String", "Long")
        check("", "", ::fooReturnLong, "String", "Any")

        check(Int::suspendToString, ::foo, "Int", "String")
    }

    return "OK"
}
