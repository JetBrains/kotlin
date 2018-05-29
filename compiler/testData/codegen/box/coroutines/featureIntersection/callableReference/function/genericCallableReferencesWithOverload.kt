// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*
import kotlin.test.assertEquals

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo(x: Int?) {}
suspend fun foo(y: String?) {}
suspend fun foo(z: Boolean) {}

suspend inline fun <reified T> bar(f: suspend (T) -> Unit, tType: String): T? {
    assertEquals(tType, T::class.simpleName)
    return null
}

fun box(): String {
    builder {
        val a1: Int? = bar(::foo, "Int")
        val a2: String? = bar(::foo, "String")
        val a3: Boolean? = bar<Boolean>(::foo, "Boolean")
    }

    return "OK"
}
