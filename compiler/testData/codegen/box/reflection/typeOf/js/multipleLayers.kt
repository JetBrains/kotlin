// TARGET_BACKEND: JS_IR
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

// FILE: lib.kt
package test

import kotlin.reflect.typeOf
import kotlin.reflect.KType

interface C

inline fun <reified T> get() = typeOf<T>()

inline fun <reified U> get1() = get<U?>()

inline fun <reified V> get2(): KType {
    return get1<Map<in V?, Array<V>>>()
}

// FILE: main.kt
package test
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("C?", get1<C>().toString())
    assertEquals("Map<in C?, Array<C>>?", get2<C>().toString())
    assertEquals("Map<in List<C>?, Array<List<C>>>?", get2<List<C>>().toString())
    return "OK"
}
