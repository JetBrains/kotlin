import kotlin.test.*


private fun doTestNumber(value: Float, isNaN: Boolean = false, isInfinite: Boolean = false) {
    assertEquals(isNaN, value.isNaN(), "Expected $value to have isNaN: $isNaN")
    assertEquals(isInfinite, value.isInfinite(), "Expected $value to have isInfinite: $isInfinite")
    assertEquals(!isNaN && !isInfinite, value.isFinite())
}

fun box() {
    for (value in listOf(1.0F, 0.0F, Float.MAX_VALUE, Float.MIN_VALUE))
        doTestNumber(value)
    for (value in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY))
        doTestNumber(value, isInfinite = true)
    doTestNumber(Float.NaN, isNaN = true)
}
