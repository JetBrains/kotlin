// ISSUE: KT-72391
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

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
