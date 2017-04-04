import kotlin.test.*


fun box() {
    val range = -1.0f..3.14159f
    assertFalse(-1e30f in range)
    assertFalse(-100.0f in range)
    assertFalse(-1.00001f in range)

    assertTrue(-1.0f in range)
    assertTrue(-0.99999f in range)
    assertTrue(0.0f in range)
    assertTrue(1.5f in range)
    assertTrue(3.1415f in range)
    assertTrue(3.14159f in range)

    assertFalse(3.15f in range)
    assertFalse(10.0f in range)
    assertFalse(1e30f in range)

    assertFalse(range.isEmpty())

    assertTrue(1.toByte() in range)
    assertTrue(1.toShort() in range)
    assertTrue(1.toInt() in range)
    assertTrue(1.toLong() in range)
    assertTrue(1.toDouble() in range)

    assertFalse(Double.MAX_VALUE in range)

    val zeroRange = 0.0F..-0.0F
    assertFalse(zeroRange.isEmpty())
    assertTrue(-0.0F in zeroRange)
    val normalZeroRange = -0.0F..0.0F
    assertEquals(zeroRange, normalZeroRange)
    assertEquals(zeroRange.hashCode(), normalZeroRange.hashCode())

    val nanRange = 0.0F..Float.NaN
    assertFalse(1.0F in nanRange)
    assertFalse(Float.NaN in nanRange)
    assertTrue(nanRange.isEmpty())

    val halfInfRange = 0.0F..Float.POSITIVE_INFINITY
    assertTrue(Float.POSITIVE_INFINITY in halfInfRange)
    assertFalse(Float.NEGATIVE_INFINITY in halfInfRange)
    assertFalse(Float.NaN in halfInfRange)
    assertTrue(Double.POSITIVE_INFINITY in halfInfRange)
    assertTrue(Double.MAX_VALUE in halfInfRange)
}
