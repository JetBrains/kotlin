// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

// FILE: lib.kt
package test

import kotlin.reflect.typeOf

interface C

inline fun <reified T> get() = typeOf<T>()

inline fun <reified U> get1() = get<U?>()

inline fun <reified V> get2() = get1<Map<in V?, Array<V>>>()

// FILE: main.kt
package test
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.collections.Map<in test.C?, kotlin.Array<test.C>>?", get2<C>().toString())
    assertEquals("kotlin.collections.Map<in kotlin.collections.List<test.C>?, kotlin.Array<kotlin.collections.List<test.C>>>?", get2<List<C>>().toString())
    return "OK"
}
