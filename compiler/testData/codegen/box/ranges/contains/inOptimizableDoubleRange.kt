// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// WITH_RUNTIME
import kotlin.test.*

fun check(x: Double, left: Double, right: Double): Boolean {
    val result = x in left..right
    val manual = x >= left && x <= right
    val range = left..right
    assertTrue(result == manual, "Failed: optimized === manual for $range")
    assertTrue(result == checkUnoptimized(x, range), "Failed: optimized === unoptimized for $range")
    return result
}

fun checkUnoptimized(x: Double, range: ClosedRange<Double>): Boolean {
    return x in range
}

fun box(): String {
    assertTrue(check(1.0, 0.0, 2.0))
    assertTrue(!check(1.0, -1.0, 0.0))

    assertTrue(check(Double.MIN_VALUE, 0.0, 1.0))
    assertTrue(check(Double.MAX_VALUE, Double.MAX_VALUE - Double.MIN_VALUE, Double.MAX_VALUE))
    assertTrue(!check(Double.NaN, Double.NaN, Double.NaN))
    assertTrue(!check(0.0, Double.NaN, Double.NaN))

    assertTrue(check(-0.0, -0.0, +0.0))
    assertTrue(check(-0.0, -0.0, -0.0))
    assertTrue(check(-0.0, +0.0, +0.0))
    assertTrue(check(+0.0, -0.0, -0.0))
    assertTrue(check(+0.0, +0.0, +0.0))
    assertTrue(check(+0.0, -0.0, +0.0))

    var value = 0.0
    assertTrue(++value in 1.0..1.0)
    assertTrue(++value !in 1.0..1.0)
    return "OK"
}
