// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

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
