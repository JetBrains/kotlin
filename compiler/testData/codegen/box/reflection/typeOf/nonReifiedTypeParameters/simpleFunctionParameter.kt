// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

fun <X1> notNull() = typeOf<Container<X1>>()
fun <X2> nullable() = typeOf<Container<X2?>>()

fun box(): String {
    assertEquals("test.Container<X1>", notNull<Any>().toString())
    assertEquals("test.Container<X2?>", nullable<Any>().toString())
    return "OK"
}
