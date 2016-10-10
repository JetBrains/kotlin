// WITH_RUNTIME

fun check(x: Float, left: Float, right: Float): Boolean {
    val result = x in left..right
    assert(result == checkUnoptimized(x, left..right))
    return result
}

fun checkUnoptimized(x: Float, range: ClosedRange<Float>): Boolean {
    return x in range
}

fun box(): String {
    assert(check(1.0f, 0.0f, 2.0f))
    assert(!check(1.0f, -1.0f, 0.0f))

    assert(check(Float.MIN_VALUE, 0.0f, 1.0f))
    assert(check(Float.MAX_VALUE, Float.MAX_VALUE - Float.MIN_VALUE, Float.MAX_VALUE))
    assert(check(Float.NaN, Float.NaN, Float.NaN))
    assert(!check(0.0f, Float.NaN, Float.NaN))

    assert(check(-0.0f, -0.0f, +0.0f))
    assert(check(-0.0f, -0.0f, -0.0f))
    assert(!check(-0.0f, +0.0f, +0.0f))
    assert(!check(+0.0f, -0.0f, -0.0f))
    assert(check(+0.0f, +0.0f, +0.0f))
    assert(check(+0.0f, -0.0f, +0.0f))

    var value = 0.0f
    assert(++value in 1.0f..1.0f)
    assert(++value !in 1.0f..1.0f)
    return "OK"
}
