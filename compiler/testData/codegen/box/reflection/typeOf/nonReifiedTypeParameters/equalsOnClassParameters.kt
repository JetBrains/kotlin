// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Container<T>

class C<X, Y> {
    val x1 = createX()
    val x2 = createXFromOtherFunction()
    val xFun = createIrrelevantX<Any>()
    val y = createY()

    fun createX(): KTypeParameter =
        typeOf<Container<X>>().arguments.single().type!!.classifier as KTypeParameter

    fun createXFromOtherFunction(): KTypeParameter =
        typeOf<Container<X>>().arguments.single().type!!.classifier as KTypeParameter

    fun <X> createIrrelevantX(): KTypeParameter =
        typeOf<Container<X>>().arguments.single().type!!.classifier as KTypeParameter

    fun createY(): KTypeParameter =
        typeOf<Container<Y>>().arguments.single().type!!.classifier as KTypeParameter
}

fun box(): String {
    val c = C<Any, Any>()
    assertEquals(c.x1, c.x2)
    assertEquals(c.x1.hashCode(), c.x2.hashCode())

    assertNotEquals(c.x1, c.xFun)
    assertNotEquals(c.x1, c.y)
    return "OK"
}
