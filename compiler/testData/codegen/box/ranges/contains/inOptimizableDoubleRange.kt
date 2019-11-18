// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun check(x: Double, left: Double, right: Double): Boolean {
    val result = x in left..right
    val manual = x >= left && x <= right
    val range = left..right
    assert(result == manual) { "Failed: optimized === manual for $range" }
    assert(result == checkUnoptimized(x, range)) { "Failed: optimized === unoptimized for $range" }
    return result
}

fun checkUnoptimized(x: Double, range: ClosedRange<Double>): Boolean {
    return x in range
}

fun box(): String {
    assert(check(1.0, 0.0, 2.0))
    assert(!check(1.0, -1.0, 0.0))

    assert(check(Double.MIN_VALUE, 0.0, 1.0))
    assert(check(Double.MAX_VALUE, Double.MAX_VALUE - Double.MIN_VALUE, Double.MAX_VALUE))
    assert(!check(Double.NaN, Double.NaN, Double.NaN))
    assert(!check(0.0, Double.NaN, Double.NaN))

    assert(check(-0.0, -0.0, +0.0))
    assert(check(-0.0, -0.0, -0.0))
    assert(check(-0.0, +0.0, +0.0))
    assert(check(+0.0, -0.0, -0.0))
    assert(check(+0.0, +0.0, +0.0))
    assert(check(+0.0, -0.0, +0.0))

    var value = 0.0
    assert(++value in 1.0..1.0)
    assert(++value !in 1.0..1.0)
    return "OK"
}
