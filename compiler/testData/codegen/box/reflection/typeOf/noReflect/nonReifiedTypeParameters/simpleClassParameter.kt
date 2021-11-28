// TARGET_BACKEND: JVM
// WITH_STDLIB

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

class C<X> {
    fun notNull() = typeOf<Container<X>>()
    fun nullable() = typeOf<Container<X?>>()
}

fun box(): String {
    assertEquals("test.Container<X> (Kotlin reflection is not available)", C<Any>().notNull().toString())
    assertEquals("test.Container<X?> (Kotlin reflection is not available)", C<Any>().nullable().toString())
    return "OK"
}
