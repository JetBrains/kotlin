import kotlin.test.*


fun box() {
    val range = -1.0..3.14159265358979
    assertFalse(-1e200 in range)
    assertFalse(-100.0 in range)
    assertFalse(-1.00000000001 in range)

    assertTrue(-1.0 in range)
    assertTrue(-0.99999999999 in range)
    assertTrue(0.0 in range)
    assertTrue(1.5 in range)
    assertTrue(3.1415 in range)
    assertTrue(3.14159265358979 in range)

    assertFalse(3.15 in range)
    assertFalse(10.0 in range)
    assertFalse(1e200 in range)

    assertFalse(range.isEmpty())

    assertTrue(1.toByte() in range)
    assertTrue(1.toShort() in range)
    assertTrue(1.toInt() in range)
    assertTrue(1.toLong() in range)
    assertTrue(1.toFloat() in range)

    val zeroRange = 0.0..-0.0
    assertFalse(zeroRange.isEmpty())
    assertTrue(-0.0 in zeroRange)
    assertTrue(-0.0F in zeroRange)
    val normalZeroRange = -0.0..0.0
    assertEquals(zeroRange, normalZeroRange)
    assertEquals(zeroRange.hashCode(), normalZeroRange.hashCode())

    val nanRange = 0.0..Double.NaN
    assertFalse(1.0 in nanRange)
    assertFalse(Double.NaN in nanRange)
    assertFalse(Float.NaN in nanRange)
    assertTrue(nanRange.isEmpty())

    val halfInfRange = 0.0..Double.POSITIVE_INFINITY
    assertTrue(Double.POSITIVE_INFINITY in halfInfRange)
    assertFalse(Double.NEGATIVE_INFINITY in halfInfRange)
    assertFalse(Double.NaN in halfInfRange)
    assertTrue(Float.POSITIVE_INFINITY in halfInfRange)
}
