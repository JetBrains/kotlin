// WITH_STDLIB

fun box(): String {
    val x: ULong = 0Xfedcba9876543210UL
    val a = "$x"
    val b = "${0Xfedcba9876543210UL}"

    return if (a == b) {
        "OK"
    } else {
        "FAIL: $b"
    }
}