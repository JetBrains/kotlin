// WITH_STDLIB
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: 2.2.0
// ^^^ KT-79704 is fixed in 2.3.0-Beta1, see improved `equals()` in kotlin.reflect.js.internal.KTypeParameterImpl, commit 4bdbc543
// ^^^ Illegal value: <X>
//     at assertNotEquals
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

    assertNotEquals(createX<Any>(), createOtherX<Any>())
    return "OK"
}
