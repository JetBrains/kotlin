// TARGET_BACKEND: JVM
// WITH_STDLIB

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

fun <X1> notNull() = typeOf<Container<X1>>()
fun <X2> nullable() = typeOf<Container<X2?>>()

fun box(): String {
    assertEquals("test.Container<X1> (Kotlin reflection is not available)", notNull<Any>().toString())
    assertEquals("test.Container<X2?> (Kotlin reflection is not available)", nullable<Any>().toString())
    return "OK"
}
