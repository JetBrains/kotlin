// TARGET_BACKEND: JVM
// WITH_STDLIB

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals

class Container<T>

fun <X, Y, Z> test() where X : Y?, Y : List<Z>, Z : Set<String>
        = typeOf<Container<X>>()

fun box(): String {
    val type = test<MutableList<Set<String>>?, MutableList<Set<String>>, Set<String>>()
    assertEquals("test.Container<X> (Kotlin reflection is not available)", type.toString())

    val x = type.arguments.single().type!!.classifier as KTypeParameter
    assertEquals("Y? (Kotlin reflection is not available)", x.upperBounds.joinToString())

    val y = x.upperBounds.single().classifier as KTypeParameter
    assertEquals("java.util.List<Z> (Kotlin reflection is not available)", y.upperBounds.joinToString())

    val z = y.upperBounds.single().arguments.single().type!!.classifier as KTypeParameter
    assertEquals("java.util.Set<java.lang.String> (Kotlin reflection is not available)", z.upperBounds.joinToString())

    return "OK"
}
