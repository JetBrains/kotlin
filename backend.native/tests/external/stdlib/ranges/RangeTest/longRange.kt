import kotlin.test.*


fun box() {
    val range = -5L..9L
    assertFalse(-10000000L in range)
    assertFalse(-6L in range)

    assertTrue(-5L in range)
    assertTrue(-4L in range)
    assertTrue(0L in range)
    assertTrue(3L in range)
    assertTrue(8L in range)
    assertTrue(9L in range)

    assertFalse(10L in range)
    assertFalse(10000000L in range)

    assertFalse(range.isEmpty())

    assertTrue(9 in (range as ClosedRange<Long>))
    assertFalse((range as ClosedRange<Long>).isEmpty())

    assertTrue(1.toByte() in range)
    assertTrue(1.toShort() in range)
    assertTrue(1.toInt() in range)
    assertTrue(1.toFloat() in range)
    assertTrue(1.toDouble() in range)

    assertFalse(Double.MAX_VALUE in range)

    val openRange = 1L until 10L
    assertTrue(9L in openRange)
    assertFalse(10L in openRange)

    assertTrue((0 until Long.MIN_VALUE).isEmpty())
    assertTrue((0L until Long.MIN_VALUE).isEmpty())

}
