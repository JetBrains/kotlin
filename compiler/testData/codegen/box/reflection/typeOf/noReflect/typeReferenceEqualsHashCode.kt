// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// WITH_RUNTIME

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

class C

fun assertEqual(a: KType, b: KType) {
    if (a != b || b != a) throw AssertionError("Fail equals: $a != $b")
    if (a.hashCode() != b.hashCode()) throw AssertionError("Fail hashCode: $a != $b")
}

fun assertNotEqual(a: KType, b: KType) {
    if (a == b || b == a) throw AssertionError("Fail equals: $a == $b")
}

inline fun <reified A, reified B> equal() {
    assertEqual(typeOf<A>(), typeOf<B>())
}

inline fun <reified A, reified B> notEqual() {
    assertNotEqual(typeOf<A>(), typeOf<B>())
}

fun box(): String {
    equal<Any, Any>()
    equal<Any?, Any?>()
    equal<String, String>()

    equal<C, C>()
    equal<C?, C?>()

    equal<List<String>, List<String>>()
    equal<Enum<AnnotationRetention>, Enum<AnnotationRetention>>()

    equal<Array<Any>, Array<Any>>()
    equal<Array<IntArray>, Array<IntArray>>()
    equal<Array<*>, Array<*>>()

    equal<Int, Int>()
    equal<Int?, Int?>()

    notEqual<Any, Any?>()
    notEqual<Any, String>()
    notEqual<List<Any>, List<Any?>>()
    notEqual<Map<in Number, BooleanArray>, Map<out Number, BooleanArray>>()
    notEqual<Array<IntArray>, Array<Array<Int>>>()

    return "OK"
}
