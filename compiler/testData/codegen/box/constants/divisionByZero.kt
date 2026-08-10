// LANGUAGE: +JsIntegerDivisionCheck

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
