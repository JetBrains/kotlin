import kotlin.test.*


fun box() {
    expect(5) { 5.coerceAtLeast(1) }
    expect(5) { 1.coerceAtLeast(5) }
    expect(1) { 5.coerceAtMost(1) }
    expect(1) { 1.coerceAtMost(5) }

    for (value in 0..10) {
        expect(value) { value.coerceIn(null, null) }
        val min = 2
        val max = 5
        val range = min..max
        expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
        expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
        expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
        expect(value.coerceAtMost(max).coerceAtLeast(min)) { value.coerceIn(range) }
        assertTrue((value.coerceIn(range)) in range)
    }

    assertFails { 1.coerceIn(1, 0) }
    assertFails { 1.coerceIn(1..0) }
}
