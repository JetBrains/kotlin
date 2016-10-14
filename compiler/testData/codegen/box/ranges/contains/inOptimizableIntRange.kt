// WITH_RUNTIME

fun check(x: Int, left: Int, right: Int): Boolean {
    val result = x in left..right
    assert(result == checkUnoptimized(x, left..right))
    return result
}

fun checkUnoptimized(x: Int, range: ClosedRange<Int>): Boolean {
    return x in range
}

fun box(): String {
    assert(check(1, 0, 2))
    assert(!check(1, -1, 0))
    assert(!check(239, 239, 238))
    assert(check(239, 238, 239))

    assert(check(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE))
    assert(check(Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE))

    var value = 0
    assert(++value in 1..1)
    assert(++value !in 1..1)
    return "OK"
}
