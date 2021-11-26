// WITH_STDLIB

fun testIn(x: ULong) =
    x in UInt.MIN_VALUE..UInt.MAX_VALUE

fun box(): String =
    if (testIn(UInt.MAX_VALUE.toULong() + 1UL))
        "Failed"
    else
        "OK"
