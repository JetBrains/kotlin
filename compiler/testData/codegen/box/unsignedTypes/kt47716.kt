// WITH_STDLIB

fun box(): String {
    val r =
        try {
            ULong
        } finally {
            UInt.MAX_VALUE
        }.MAX_VALUE
    return if (r == ULong.MAX_VALUE) "OK" else "FAIL"
}
