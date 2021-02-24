// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals

class Container<T>

fun <X, Y, Z> test() where X : Y?, Y : List<Z>, Z : Set<String>
        = typeOf<Container<X>>()

fun box(): String {
    val type = test<MutableList<Set<String>>?, MutableList<Set<String>>, Set<String>>()
    val containerNmae = className("test.Container")
    assertEquals("$containerNmae<X>", type.toString())

    val x = type.arguments.single().type!!.classifier as KTypeParameter
    assertEquals("Y?", x.upperBounds.joinToString())

    val y = x.upperBounds.single().classifier as KTypeParameter
    val listName = className("kotlin.collections.List")
    assertEquals("$listName<Z>", y.upperBounds.joinToString())

    val z = y.upperBounds.single().arguments.single().type!!.classifier as KTypeParameter
    val setName = className("kotlin.collections.Set")
    val stringName = className("kotlin.String")
    assertEquals("$setName<$stringName>", z.upperBounds.joinToString())

    return "OK"
}

fun className(fqName: String): String {
    val isJS = 1 as Any is Double
    return if (isJS) fqName.substringAfterLast('.') else fqName
}
