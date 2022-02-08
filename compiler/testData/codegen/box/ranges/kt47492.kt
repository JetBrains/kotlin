// WITH_STDLIB

fun p() {}

fun box(): String {
    var sum = 1
    for (i: Int? in sum downTo sum.toULong().countTrailingZeroBits())
        p()
    return "OK"
}
