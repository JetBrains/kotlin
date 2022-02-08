// TARGET_BACKEND: JVM
// WITH_STDLIB

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
    assertEquals("Y (Kotlin reflection is not available)", z.upperBounds.joinToString())
    val y = z.upperBounds.single().classifier as KTypeParameter
    assertEquals("X (Kotlin reflection is not available)", y.upperBounds.joinToString())
    return "OK"
}
