import kotlin.test.*


fun box() {
    val range = (-5).toShort()..9.toShort()
    assertFalse((-1000).toShort() in range)
    assertFalse((-6).toShort() in range)

    assertTrue((-5).toShort() in range)
    assertTrue((-4).toShort() in range)
    assertTrue(0.toShort() in range)
    assertTrue(3.toShort() in range)
    assertTrue(8.toShort() in range)
    assertTrue(9.toShort() in range)

    assertFalse(10.toShort() in range)
    assertFalse(239.toShort() in range)

    assertFalse(range.isEmpty())

    assertTrue(1.toByte() in range)
    assertTrue(1.toInt() in range)
    assertTrue(1.toLong() in range)
    assertTrue(1.toFloat() in range)
    assertTrue(1.toDouble() in range)

    assertFalse(Long.MAX_VALUE in range)

    val openRange = 1.toShort() until 10.toShort()
    assertTrue(9.toShort() in openRange)
    assertFalse(10.toShort() in openRange)

    assertTrue((0.toShort() until Short.MIN_VALUE).isEmpty())
    assertTrue((0.toShort() until Int.MIN_VALUE).isEmpty())
}
