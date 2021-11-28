// WITH_STDLIB
import kotlin.test.*

fun check(x: Float, left: Float, right: Float): Boolean {
    val result = x in left..right
    val manual = x >= left && x <= right
    val range = left..right
    assertTrue(result == manual, "Failed: optimized === manual for $range")
    assertTrue(result == checkUnoptimized(x, range), "Failed: optimized === unoptimized for $range")
    return result
}

fun checkUnoptimized(x: Float, range: ClosedRange<Float>): Boolean {
    return x in range
}

fun box(): String {
    assertTrue(check(1.0f, 0.0f, 2.0f))
    assertTrue(!check(1.0f, -1.0f, 0.0f))

    assertTrue(check(Float.MIN_VALUE, 0.0f, 1.0f))
    assertTrue(check(Float.MAX_VALUE, Float.MAX_VALUE - Float.MIN_VALUE, Float.MAX_VALUE))
    assertTrue(!check(Float.NaN, Float.NaN, Float.NaN))
    assertTrue(!check(0.0f, Float.NaN, Float.NaN))

    assertTrue(check(-0.0f, -0.0f, +0.0f))
    assertTrue(check(-0.0f, -0.0f, -0.0f))
    assertTrue(check(-0.0f, +0.0f, +0.0f))
    assertTrue(check(+0.0f, -0.0f, -0.0f))
    assertTrue(check(+0.0f, +0.0f, +0.0f))
    assertTrue(check(+0.0f, -0.0f, +0.0f))

    var value = 0.0f
    assertTrue(++value in 1.0f..1.0f)
    assertTrue(++value !in 1.0f..1.0f)
    return "OK"
}
