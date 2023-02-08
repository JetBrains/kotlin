// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JVM_IR, NATIVE, JS_IR, JS_IR_ES6

const val flag = true
const val value = 10
const val condition = if (flag) "True" else "Error"
const val withWhen = when (flag) {
    true -> "True"
    else -> "Error"
}
const val withWhen2 = when {
    flag == true -> "True"
    else -> "Error"
}
const val withWhen3 = when(value) {
    10 -> "1"
    100 -> "2"
    else -> "3"
}
const val multibranchIf = if (value == 100) 1 else if (value == 1000) 2 else 3

fun box(): String {
    if (condition != "True") return "Fail 1"
    if (withWhen != "True") return "Fail 2"
    if (withWhen2 != "True") return "Fail 3"
    if (withWhen3 != "1") return "Fail 4"
    if (multibranchIf != 3) return "Fail 5"
    return "OK"
}
