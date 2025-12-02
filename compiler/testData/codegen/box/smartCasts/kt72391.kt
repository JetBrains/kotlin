// ISSUE: KT-72391

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: 1.9_JS 2.0_JS 2.1_JS 2.2.0_JS
// ^^^ KT-72391 fixed in 2.2.20-Beta1

fun multiply(a: Int, b: Long?): Double {
    if (b == null) {
        return 0.0
    }
    return a * b * 10.0
}

fun box(): String {
    val result = multiply(5, 6)
    if (result == 300.0) return "OK"
    return result.toString()
}
