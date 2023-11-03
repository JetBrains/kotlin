// IGNORE_BACKEND: JVM, JS
// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// 0 INVOKESTATIC kotlin/ranges/RangesKt.step
// 0 INVOKESTATIC kotlin/ranges/URangesKt.step

import kotlin.test.*

fun box(): String {
    // Byte
    var count = 0
    for (i in (0.toByte() until 3.toByte() step 2)) {
        count += i
    }
    assertEquals(2, count)

    // Short
    count = 0
    for (i in (0.toShort() until 3.toShort() step 2)) {
        count += i
    }
    assertEquals(2, count)

    // Int
    count = 0
    for (i in 0 until 3 step 2) {
        count += i
    }
    assertEquals(2, count)

    // UInt
    count = 0
    for (i in 0u until 3u step 2) {
        count += i.toInt()
    }
    assertEquals(2, count)

    // Long
    count = 0
    for (i in 0L until 3L step 2) {
        count += i.toInt()
    }
    assertEquals(2, count)

    // ULong
    count = 0
    for (i in 0UL until 3UL step 2L) {
        count += i.toInt()
    }
    assertEquals(2, count)

    // Char
    var last = ' '
    for (i in '0' until 'C' step 2) {
        last = i
    }
    assertEquals('B', last)

    return "OK"
}