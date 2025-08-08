// ISSUE: KT-72391
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0 2.1.0 2.2.0
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
