// TARGET_BACKEND: NATIVE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.reflect.*


class Box(@Volatile var value1: Int, @Volatile var value2: Int)

inline fun KMutableProperty0<Int>.wrapCas(expected: Int, new: Int) = this.compareAndSwapField(expected, new)
inline fun wrapCasTwice(expected: Int, new: Int, property: KMutableProperty0<Int>) = property.wrapCas(expected, new)

fun box() : String {
    val a = Box(1, 2)
    (a::value1).wrapCas(1, 3)
    (a::value2).wrapCas(2, 4)
    if (a.value1 != 3) return "FAIL 1"
    if (a.value2 != 4) return "FAIL 2"
    wrapCasTwice(3, 5, a::value1)
    wrapCasTwice( 4, 6, a::value2)
    if (a.value1 != 5) return "FAIL 3"
    if (a.value2 != 6) return "FAIL 4"
    return "OK"
}
