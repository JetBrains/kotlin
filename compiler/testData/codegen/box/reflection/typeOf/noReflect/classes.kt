// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class C

fun check(expected: String, actual: KType) {
    assertEquals(expected + " (Kotlin reflection is not available)", actual.toString())
}

fun box(): String {
    check("java.lang.Object", typeOf<Any>())
    check("java.lang.String", typeOf<String>())
    check("java.lang.String?", typeOf<String?>())
    check("kotlin.Unit", typeOf<Unit>())

    check("test.C", typeOf<C>())
    check("test.C?", typeOf<C?>())

    check("java.util.List<java.lang.String>", typeOf<List<String>>())
    check("java.util.Map<in java.lang.Number, java.lang.Object?>?", typeOf<Map<in Number, *>?>())
    check("java.lang.Enum<out java.lang.Enum<*>>", typeOf<Enum<*>>())
    check("java.lang.Enum<kotlin.annotation.AnnotationRetention>", typeOf<Enum<AnnotationRetention>>())

    check("kotlin.Array<java.lang.Object>", typeOf<Array<Any>>())
    check("kotlin.Array<out java.lang.Object?>", typeOf<Array<*>>())
    check("kotlin.Array<kotlin.IntArray>", typeOf<Array<IntArray>>())
    check("kotlin.Array<in kotlin.Array<test.C>?>", typeOf<Array<in Array<C>?>>())

    check("int", typeOf<Int>())
    check("java.lang.Integer?", typeOf<Int?>())
    check("boolean", typeOf<Boolean>())

    return "OK"
}
