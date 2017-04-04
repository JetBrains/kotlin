import kotlin.test.*


fun box() {
    val range = (-5).toByte()..9.toByte()
    assertFalse((-100).toByte() in range)
    assertFalse((-6).toByte() in range)

    assertTrue((-5).toByte() in range)
    assertTrue((-4).toByte() in range)
    assertTrue(0.toByte() in range)
    assertTrue(3.toByte() in range)
    assertTrue(8.toByte() in range)
    assertTrue(9.toByte() in range)

    assertFalse(10.toByte() in range)
    assertFalse(111.toByte() in range)

    assertFalse(range.isEmpty())


    assertTrue(1.toShort() in range)
    assertTrue(1.toInt() in range)
    assertTrue(1.toLong() in range)
    assertTrue(1.toFloat() in range)
    assertTrue(1.toDouble() in range)

    assertFalse(Long.MAX_VALUE in range)

    val openRange = 1.toByte() until 10.toByte()
    assertTrue(9.toByte() in openRange)
    assertFalse(10.toByte() in openRange)

    // byte arguments now construct IntRange so no overflow here
    assertTrue((0.toByte() until Byte.MIN_VALUE).isEmpty())
    assertTrue((0.toByte() until Int.MIN_VALUE).isEmpty())
}
