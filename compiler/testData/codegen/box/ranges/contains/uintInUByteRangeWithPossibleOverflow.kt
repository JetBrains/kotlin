// WITH_STDLIB

fun box(): String {
    val x1 = 1U
    if (x1 !in UByte.MIN_VALUE..UByte.MAX_VALUE)
        return "Failed"
    return "OK"
}