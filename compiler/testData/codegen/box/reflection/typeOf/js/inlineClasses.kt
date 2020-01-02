// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

inline class Z(val value: String)

fun check(expected: String, actual: KType) {
    assertEquals(expected, actual.toString())
}

fun box(): String {
    check("Z", typeOf<Z>())
    check("Z?", typeOf<Z?>())
    check("Array<Z>", typeOf<Array<Z>>())
    check("Array<Z?>", typeOf<Array<Z?>>())

    check("UInt", typeOf<UInt>())
    check("UInt?", typeOf<UInt?>())
    check("ULong?", typeOf<ULong?>())
    check("UShortArray", typeOf<UShortArray>())
    check("UShortArray?", typeOf<UShortArray?>())
    check("Array<UByteArray>", typeOf<Array<UByteArray>>())
    check("Array<UByteArray?>?", typeOf<Array<UByteArray?>?>())

    return "OK"
}
