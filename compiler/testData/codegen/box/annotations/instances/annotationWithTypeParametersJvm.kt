// TARGET_BACKEND: JVM_IR

// WITH_REFLECT
// !LANGUAGE: +InstantiationOfAnnotationClasses

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue as assert

annotation class WithKClass<K: Any>(val k: KClass<K>)

// Following can't be called on JS/Native, as on these platforms only `Array::class : KClass<Array<*>>`  exists.

annotation class WithArray<A>(val a: KClass<Array<A>>)

annotation class Combined<T: Any>(val t: KClass<T>, val w: WithKClass<T>, val wa: WithArray<T>)

class X

fun box(): String {
    val wa = WithArray(Array<String>::class)
    assert(wa.a.java.isArray)
    assert(wa.a.java.componentType == String::class.java)
    assertEquals(WithArray(Array<String>::class), wa)
    assert(WithArray(Array<Int>::class) != wa)

    val typeParams = WithKClass(X::class).annotationClass.typeParameters
    assertEquals(1, typeParams.size)

    val c = Combined(X::class, WithKClass(X::class), WithArray(Array<X>::class))
    assertEquals(X::class, c.t)
    assertEquals(X::class, c.w.k)
    assertEquals(X::class.java, c.wa.a.java.componentType)

    return "OK"
}
