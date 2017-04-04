import kotlin.test.*


fun box() {
    assertTrue(Float.MIN_VALUE > 0)
    assertTrue(Float.MAX_VALUE > 0)

    // overflow behavior
    expect(Float.POSITIVE_INFINITY) { Float.MAX_VALUE * 2 }
    expect(Float.NEGATIVE_INFINITY) { -Float.MAX_VALUE * 2 }
    expect(0.0F) { Float.MIN_VALUE / 2.0F }
}
