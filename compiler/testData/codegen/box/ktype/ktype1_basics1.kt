// AssertionError: Expected <C<kotlin.Int?>>, actual <C<Int?>>.
// AssertionError: Expected <C<C<kotlin.Any>>>, actual <C<C<Any>>>.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, ANDROID

// WITH_STDLIB
// WITH_REFLECT

import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalStdlibApi::class)
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

fun box(): String {
    assertEquals("C<kotlin.Int?>", kType<C<Int?>>().toString())
    assertEquals("C<C<kotlin.Any>>", kType<C<C<Any>>>().toString())

    assertEquals("C<T>", kTypeForCWithTypeParameter<D>().toString())
    assertEquals("Object", kType<Object>().toString())
    assertEquals("Outer.Friend", kType<Outer.Friend>().toString())

    return "OK"
}
