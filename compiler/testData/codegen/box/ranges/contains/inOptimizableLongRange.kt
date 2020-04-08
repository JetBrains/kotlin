// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun check(x: Long, left: Long, right: Long): Boolean {
    val result = x in left..right
    val manual = x >= left && x <= right
    val range = left..right
    assert(result == manual) { "Failed: optimized === manual for $range" }
    assert(result == checkUnoptimized(x, range)) { "Failed: optimized === unoptimized for $range" }
    return result
}

fun checkUnoptimized(x: Long, range: ClosedRange<Long>): Boolean {
    return x in range
}

fun box(): String {
    assert(check(1L, 0L, 2L))
    assert(!check(1L, -1L, 0L))
    assert(!check(239L, 239L, 238L))
    assert(check(239L, 238L, 239L))

    assert(check(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE))
    assert(check(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE))

    var value = 0L
    assert(++value in 1L..1L)
    assert(++value !in 1L..1L)
    return "OK"
}
