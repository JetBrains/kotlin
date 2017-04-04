import kotlin.test.*


fun box() {
    expect(5L) { 5L.coerceAtLeast(1L) }
    expect(5L) { 1L.coerceAtLeast(5L) }
    expect(1L) { 5L.coerceAtMost(1L) }
    expect(1L) { 1L.coerceAtMost(5L) }

    for (value in 0L..10L) {
        expect(value) { value.coerceIn(null, null) }
        val min = 2L
        val max = 5L
        val range = min..max
        expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
        expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
        expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
        expect(value.coerceAtMost(max).coerceAtLeast(min)) { value.coerceIn(range) }
        assertTrue((value.coerceIn(range)) in range)
    }

    assertFails { 1L.coerceIn(1L, 0L) }
    assertFails { 1L.coerceIn(1L..0L) }

}
