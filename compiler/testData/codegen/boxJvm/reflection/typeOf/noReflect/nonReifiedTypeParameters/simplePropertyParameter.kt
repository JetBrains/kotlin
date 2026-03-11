// TARGET_BACKEND: JVM
// WITH_STDLIB

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

val <X1> X1.notNull get() = typeOf<Container<X1>>()
val <X2> X2.nullable get() = typeOf<Container<X2?>>()

fun box(): String {
    assertEquals("test.Container<X1> (Kotlin reflection is not available)", "".notNull.toString())
    assertEquals("test.Container<X2?> (Kotlin reflection is not available)", "".nullable.toString())
    return "OK"
}
