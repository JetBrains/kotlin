// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun check(x: Int, left: Int, right: Int): Boolean {
    val result = x in left..right
    val manual = x >= left && x <= right
    val range = left..right
    assert(result == manual) { "Failed: optimized === manual for $range" }
    assert(result == checkUnoptimized(x, range)) { "Failed: optimized === unoptimized for $range" }
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
