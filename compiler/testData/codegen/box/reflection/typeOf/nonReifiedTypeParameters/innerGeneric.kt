// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.KTypeParameter
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

class C<X> {
    inner class D<Y : X> {
        fun <Z : Y> createZ(): KTypeParameter =
            typeOf<Container<Z>>().arguments.single().type!!.classifier as KTypeParameter
    }
}

fun box(): String {
    val z = C<Any>().D<Any>().createZ<Any>()
    assertEquals("Y", z.upperBounds.joinToString())
    val y = z.upperBounds.single().classifier as KTypeParameter
    assertEquals("X", y.upperBounds.joinToString())
    return "OK"
}
