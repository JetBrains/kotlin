// WITH_RUNTIME

fun check(x: Long, left: Long, right: Long): Boolean {
    val result = x in left..right
    assert(result == checkUnoptimized(x, left..right))
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
