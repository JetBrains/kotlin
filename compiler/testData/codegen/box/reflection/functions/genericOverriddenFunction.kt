// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

interface H<T> {
    fun foo(): T?
}

interface A : H<A>

fun box(): String {
    assertEquals("A?", A::foo.returnType.toString())
    assertEquals("T?", H<A>::foo.returnType.toString())

    return "OK"
}
