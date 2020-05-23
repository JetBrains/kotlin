// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

val <X1> X1.notNull get() = typeOf<Container<X1>>()
val <X2> X2.nullable get() = typeOf<Container<X2?>>()

fun box(): String {
    assertEquals("test.Container<X1>", "".notNull.toString())
    assertEquals("test.Container<X2?>", "".nullable.toString())
    return "OK"
}
