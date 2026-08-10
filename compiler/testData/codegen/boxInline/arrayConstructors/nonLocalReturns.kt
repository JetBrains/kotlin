// WITH_STDLIB
// FILE: inline.kt

inline fun test1(n: Int): Array<String>? = Array(n) {
    if (it > 3) return null
    "!"
}

inline fun test2(n: Int): DoubleArray? = DoubleArray(n) {
    if (it > 3) return@test2 null
    42.0
}

inline fun test3(arr: Array<String>): String {
    var x = emptyArray<String>()
    for (element in arr) {
        x += Array(element.length) { if (it > 3) continue; element }
    }
    return x.joinToString("")
}

typealias MyDoubleArray = Array<Double>

inline fun test4(n: Int): MyDoubleArray? = MyDoubleArray(n) {
    if (it > 3) return null
    42.0
}

// FILE: inlineSite.kt

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

fun box(): String {
    assertContentEquals(arrayOf("!", "!", "!", "!"), test1(4))
    assertEquals(null, test1(5))
    assertContentEquals(doubleArrayOf(42.0, 42.0, 42.0, 42.0), test2(4))
    assertEquals(null, test2(5))
    assertEquals("zzzzzzzzzx", test3(arrayOf("zzz", "ttttt", "x")))
    assertContentEquals(arrayOf(42.0, 42.0, 42.0, 42.0), test4(4))
    assertEquals(null, test4(5))
    return "OK"
}
