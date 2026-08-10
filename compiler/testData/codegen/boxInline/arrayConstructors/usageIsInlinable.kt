// WITH_STDLIB
// FILE: inline.kt

inline fun usage(size: Int, initBlock: (Int) -> Long) = LongArray(size, initBlock)
inline fun <reified T> genericUsage(size: Int, initBlock: (Int) -> T) = Array(size, initBlock)

// FILE: inlineSite.kt
import kotlin.test.assertEquals

fun box(): String {
    val longArray = usage(10) { if (it >= 5) it.toLong() - 9 else it.toLong() }
    assertEquals(longArray.sum(), 0L)
    val OK = genericUsage(2) { if (it == 1) 'K' else 'O' }
    return OK.joinToString("")
}
