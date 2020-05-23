// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals

class Container<T>

class B<W>

class C<X> {
    val <Y> B<Y>.createY: KTypeParameter where Y : X
        get() = typeOf<Container<Y>>().arguments.single().type!!.classifier as KTypeParameter
}

fun box(): String {
    with(C<Any>()) {
        val y = B<Any>().createY
        assertEquals("X", y.upperBounds.joinToString())
    }
    return "OK"
}
