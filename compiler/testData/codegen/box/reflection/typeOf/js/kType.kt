// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

import kotlin.test.*
import kotlin.reflect.*

inline fun <reified R> kType() = typeOf<R>()

inline fun <reified R> kType(obj: R) = kType<R>()

class C<T>
class D

fun <T> kTypeForCWithTypeParameter() = kType<C<T>>()

class Outer<T> {
    companion object Friend
    inner class Inner<S>
}

object Object

fun testBasics1() {
    assertEquals("C<Int?>", kType<C<Int?>>().toString())
    assertEquals("C<C<Any>>", kType<C<C<Any>>>().toString())

    assertEquals("C<T>", kTypeForCWithTypeParameter<D>().toString())
    assertEquals("Object", kType<Object>().toString())
    assertEquals("Friend", kType<Outer.Friend>().toString())
}

fun testInner() {
    val innerKType = kType<Outer<D>.Inner<String>>()
    assertEquals(Outer.Inner::class, innerKType.classifier)
    assertEquals(String::class, innerKType.arguments.first().type!!.classifier)
    assertEquals(D::class, innerKType.arguments.last().type!!.classifier)
}

fun testAnonymousObject() {
    val obj = object {}
    val objType = kType(obj)

    assertEquals("(non-denotable type)", objType.toString())
    assertEquals(obj::class, objType.classifier)

    assertTrue(objType.arguments.isEmpty())
    assertFalse(objType.isMarkedNullable)
}

fun box(): String {
    testBasics1()
    testInner()
    testAnonymousObject()
    return "OK"
}