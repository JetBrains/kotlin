import kotlin.test.*


fun box() {
    assertTrue(Double.MIN_VALUE > 0)
    assertTrue(Double.MAX_VALUE > 0)

    // overflow behavior
    expect(Double.POSITIVE_INFINITY) { Double.MAX_VALUE * 2 }
    expect(Double.NEGATIVE_INFINITY) { -Double.MAX_VALUE * 2 }
    expect(0.0) { Double.MIN_VALUE / 2 }
}
