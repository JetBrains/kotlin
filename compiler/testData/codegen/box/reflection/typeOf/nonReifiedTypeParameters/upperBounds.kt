// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals

class Container<T>

fun <X, Y, Z> test() where X : Y?, Y : List<Z>, Z : Set<String>
        = typeOf<Container<X>>()

fun box(): String {
    val type = test<MutableList<Set<String>>?, MutableList<Set<String>>, Set<String>>()
    assertEquals("test.Container<X>", type.toString())

    val x = type.arguments.single().type!!.classifier as KTypeParameter
    assertEquals("Y?", x.upperBounds.joinToString())

    val y = x.upperBounds.single().classifier as KTypeParameter
    assertEquals("kotlin.collections.List<Z>", y.upperBounds.joinToString())

    val z = y.upperBounds.single().arguments.single().type!!.classifier as KTypeParameter
    assertEquals("kotlin.collections.Set<kotlin.String>", z.upperBounds.joinToString())

    return "OK"
}
