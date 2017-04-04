import kotlin.test.*


private class ComparableNumber(val value: Int) : Comparable<ComparableNumber> {
    override fun compareTo(other: ComparableNumber): Int = this.value - other.value
    override fun toString(): String = "CV$value"
}

fun box() {
    val v = (0..10).map { ComparableNumber(it) }

    expect(5) { v[5].coerceAtLeast(v[1]).value }
    expect(5) { v[1].coerceAtLeast(v[5]).value }
    expect(v[5]) { v[5].coerceAtLeast(ComparableNumber(5)) }

    expect(1) { v[5].coerceAtMost(v[1]).value }
    expect(1) { v[1].coerceAtMost(v[5]).value }
    expect(v[1]) { v[1].coerceAtMost(ComparableNumber(1)) }

    for (value in v) {
        expect(value) { value.coerceIn(null, null) }
        val min = v[2]
        val max = v[5]
        val range = min..max
        expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
        expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
        expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
        expect(value.coerceAtMost(max).coerceAtLeast(min)) { value.coerceIn(range) }
        assertTrue((value.coerceIn(range)) in range)
    }

    assertFails { v[1].coerceIn(v[1], v[0]) }
    assertFails { v[1].coerceIn(v[1]..v[0]) }
}
