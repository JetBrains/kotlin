import kotlin.test.*


fun box() {
    expect(5.0) { 5.0.coerceAtLeast(1.0) }
    expect(5.0) { 1.0.coerceAtLeast(5.0) }
    //assertTrue { Double.NaN.coerceAtLeast(1.0).isNaN() }

    expect(1.0) { 5.0.coerceAtMost(1.0) }
    expect(1.0) { 1.0.coerceAtMost(5.0) }
    //assertTrue { Double.NaN.coerceAtMost(5.0).isNaN() }

    for (value in (0..10).map { it.toDouble() }) {
        expect(value) { value.coerceIn(null, null) }
        val min = 2.0
        val max = 5.0
        val range = min..max
        expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
        expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
        expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
        expect(value.coerceAtMost(max).coerceAtLeast(min)) { value.coerceIn(range) }
        assertTrue((value.coerceIn(range)) in range)
    }

    assertFails { 1.0.coerceIn(1.0, 0.0) }
    assertFails { 1.0.coerceIn(1.0..0.0) }

    assertTrue(0.0.equals(0.0.coerceIn(0.0, -0.0)))
    assertTrue((-0.0).equals((-0.0).coerceIn(0.0..-0.0)))

    //assertTrue(Double.NaN.coerceIn(0.0, 1.0).isNaN())
    //assertTrue(Double.NaN.coerceIn(0.0..1.0).isNaN())
}
