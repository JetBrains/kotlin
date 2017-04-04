import kotlin.test.*


fun box() {
    val range = -5..9
    assertFalse(-1000 in range)
    assertFalse(-6 in range)

    assertTrue(-5 in range)
    assertTrue(-4 in range)
    assertTrue(0 in range)
    assertTrue(3 in range)
    assertTrue(8 in range)
    assertTrue(9 in range)

    assertFalse(10 in range)
    assertFalse(9000 in range)

    assertFalse(range.isEmpty())

    assertTrue(9 in (range as ClosedRange<Int>))
    assertFalse((range as ClosedRange<Int>).isEmpty())

    assertTrue(1.toShort() in range)
    assertTrue(1.toByte() in range)
    assertTrue(1.toLong() in range)
    assertTrue(1.toFloat() in range)
    assertTrue(1.toDouble() in range)

    assertFalse(Long.MAX_VALUE in range)

    val openRange = 1 until 10
    assertTrue(9 in openRange)
    assertFalse(10 in openRange)

    assertTrue((1 until Int.MIN_VALUE).isEmpty())
}
