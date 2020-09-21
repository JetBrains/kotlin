// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals

class Container<T>

fun <X> test() = typeOf<Container<X>>()

fun box(): String {
    val type = test<Any>()
    val x = type.arguments.single().type!!.classifier as KTypeParameter

    val expected = className("kotlin.Any?")
    assertEquals(expected, x.upperBounds.joinToString())

    return "OK"
}

fun className(fqName: String): String {
    val isJS = 1 as Any is Double
    return if (isJS) fqName.substringAfterLast('.') else fqName
}
