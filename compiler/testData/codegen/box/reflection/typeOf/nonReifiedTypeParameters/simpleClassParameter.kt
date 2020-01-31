// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

class C<X> {
    fun notNull() = typeOf<Container<X>>()
    fun nullable() = typeOf<Container<X?>>()
}

fun box(): String {
    assertEquals("test.Container<X>", C<Any>().notNull().toString())
    assertEquals("test.Container<X?>", C<Any>().nullable().toString())
    return "OK"
}
