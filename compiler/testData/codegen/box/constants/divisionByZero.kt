// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS
// reason - no error from division by zero in JS

fun expectArithmeticException(f: () -> Unit): Boolean {
    try {
        f()
    } catch (e: ArithmeticException) {
        return true
    }
    return false
}

fun box(): String {
    if (!expectArithmeticException { 1 / 0 })
        return "fail: 1 / 0 didn't throw exception"

    if (!expectArithmeticException { 1 * 2 / 0 })
        return "fail: 1 * 2 / 0 didn't throw exception"

    if (!expectArithmeticException { 1 * (2 / 0) })
        return "fail: 1 * (2 / 0) didn't throw exception"

    return "OK"
}