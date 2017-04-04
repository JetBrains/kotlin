import kotlin.test.*

private fun doTestNumber(value: Double, isNaN: Boolean = false, isInfinite: Boolean = false) {
    assertEquals(isNaN, value.isNaN(), "Expected $value to have isNaN: $isNaN")
    assertEquals(isInfinite, value.isInfinite(), "Expected $value to have isInfinite: $isInfinite")
    assertEquals(!isNaN && !isInfinite, value.isFinite())
}

fun box() {
    for (value in listOf(1.0, 0.0, Double.MIN_VALUE, Double.MAX_VALUE))
        doTestNumber(value)
    for (value in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
        doTestNumber(value, isInfinite = true)
    doTestNumber(Double.NaN, isNaN = true)
}
