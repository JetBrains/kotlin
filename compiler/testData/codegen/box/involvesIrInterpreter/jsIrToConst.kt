// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6

const val toStringInConst = 1.0.toString()

fun box(): String {
    if (toStringInConst != "1") return "Fail 1"
    if (1.0.toString() != toStringInConst) return "Fail 2"
    return "OK"
}
