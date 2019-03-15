// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

class ComparablePair<T : Comparable<T>>(val first: T, val second: T) : Comparable<ComparablePair<T>> {
    override fun compareTo(other: ComparablePair<T>): Int {
        val result = first.compareTo(other.first)
        return if (result != 0) result else second.compareTo(other.second)
    }
}

fun <T : Comparable<T>> genericRangeTo(start: T, endInclusive: T) = start..endInclusive
operator fun Double.rangeTo(other: Double) = genericRangeTo(this, other)
// some weird inverted range
operator fun Float.rangeTo(other: Float) = object : ClosedFloatingPointRange<Float> {
    override val endInclusive: Float = this@rangeTo
    override val start: Float = other
    override fun lessThanOrEquals(a: Float, b: Float) = a >= b
}

fun check(x: Double, left: Double, right: Double): Boolean {
    val result = x in left..right
    val range = left..right
    assert(result == x in range) { "Failed: unoptimized === unoptimized for custom double $range" }
    return result
}

fun check(x: Float, left: Float, right: Float): Boolean {
    val result = x in left..right
    val range = left..right
    assert(result == x in range) { "Failed: unoptimized === unoptimized for standard float $range" }
    return result
}

fun box(): String {
    assert("a" !in "b".."c")
    assert("b" in "a".."d")

    assert(ComparablePair(2, 2) !in ComparablePair(1, 10)..ComparablePair(2, 1))
    assert(ComparablePair(2, 2) in ComparablePair(2, 0)..ComparablePair(2, 10))

    assert(!check(-0.0, 0.0, 0.0))
    assert(check(Double.NaN, Double.NaN, Double.NaN))

    assert(check(-0.0f, 0.0f, 0.0f))
    assert(!check(Float.NaN, Float.NaN, Float.NaN))

    return "OK"
}
