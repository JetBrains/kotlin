// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class C

fun check(expected: String, actual: KType) {
    assertEquals(expected, actual.toString())
}

fun box(): String {
    check("kotlin.Any", typeOf<Any>())
    check("kotlin.String", typeOf<String>())
    check("kotlin.String?", typeOf<String?>())
    check("kotlin.Unit", typeOf<Unit>())

    check("test.C", typeOf<C>())
    check("test.C?", typeOf<C?>())

    check("kotlin.collections.List<kotlin.String>", typeOf<List<String>>())
    check("kotlin.collections.Map<in kotlin.Number, *>?", typeOf<Map<in Number, *>?>())
    check("kotlin.Enum<*>", typeOf<Enum<*>>())
    check("kotlin.Enum<kotlin.annotation.AnnotationRetention>", typeOf<Enum<AnnotationRetention>>())

    check("kotlin.Array<kotlin.Any>", typeOf<Array<Any>>())
    check("kotlin.Array<*>", typeOf<Array<*>>())
    check("kotlin.Array<kotlin.IntArray>", typeOf<Array<IntArray>>())
    check("kotlin.Array<in kotlin.Array<test.C>?>", typeOf<Array<in Array<C>?>>())

    check("kotlin.Int", typeOf<Int>())
    check("kotlin.Int?", typeOf<Int?>())
    check("kotlin.Boolean", typeOf<Boolean>())

    return "OK"
}
