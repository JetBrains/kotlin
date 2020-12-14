// IGNORE_BACKEND: JS, JS_IR_ES6

var a = 12

object C {
    const val x = 42
}

fun getC(): C {
    a = 123
    return C
}

fun box(): String {
    val field = getC().x
    val expectedResult = 123
    if (a == expectedResult)
        return "OK"
    else
        return "FAIL"
}
