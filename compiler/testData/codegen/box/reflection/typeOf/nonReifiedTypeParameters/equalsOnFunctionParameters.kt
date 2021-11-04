// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: WASM
package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Container<T>

fun <X> createX(): KTypeParameter =
    typeOf<Container<X>>().arguments.single().type!!.classifier as KTypeParameter

fun <X> createOtherX(): KTypeParameter =
    typeOf<Container<X>>().arguments.single().type!!.classifier as KTypeParameter

fun box(): String {
    assertEquals(createX<Any>(), createX<Any>())
    assertEquals(createX<Any>().hashCode(), createX<Any>().hashCode())

    if (!isJS) {
        assertNotEquals(createX<Any>(), createOtherX<Any>())
    }
    return "OK"
}

val isJS = 1 as Any is Double
