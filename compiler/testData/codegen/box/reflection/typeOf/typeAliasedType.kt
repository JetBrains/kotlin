// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// Should be unmuted for JS when KT-79471 is fixed

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

typealias T1 = String
typealias T2<X> = List<X>
typealias T3<X, Y> = MutableMap<in Y, X?>

fun box(): String {
    // Should be `kotlin.String /* = test.T1 */` (KT-70631).
    assertEquals("kotlin.String", typeOf<T1>().toString())
    assertEquals("kotlin.collections.List<kotlin.Any>", typeOf<T2<Any>>().toString())
    assertEquals("kotlin.collections.MutableMap<in kotlin.String, kotlin.Int?>", typeOf<T3<Int, T1>>().toString())

    return "OK"
}
