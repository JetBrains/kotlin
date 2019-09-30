// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class C

fun check(expected: String, actual: KType) {
    assertEquals(expected, actual.toString())
}

fun box(): String {
    check("Any", typeOf<Any>())
    check("String", typeOf<String>())
    check("String?", typeOf<String?>())
    check("Unit", typeOf<Unit>())

    check("C", typeOf<C>())
    check("C?", typeOf<C?>())

    check("List<String>", typeOf<List<String>>())
    check("Map<in Number, *>?", typeOf<Map<in Number, *>?>())
    check("Enum<*>", typeOf<Enum<*>>())
    check("Enum<AnnotationRetention>", typeOf<Enum<AnnotationRetention>>())

    check("Array<Any>", typeOf<Array<Any>>())
    check("Array<*>", typeOf<Array<*>>())
    check("Array<IntArray>", typeOf<Array<IntArray>>())
    check("Array<in Array<C>?>", typeOf<Array<in Array<C>?>>())

    check("Int", typeOf<Int>())
    check("Int?", typeOf<Int?>())
    check("Boolean", typeOf<Boolean>())

    return "OK"
}
