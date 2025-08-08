// WITH_STDLIB

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0
// ^^^ KT-69495 fixed in 2.0.20-Beta2

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