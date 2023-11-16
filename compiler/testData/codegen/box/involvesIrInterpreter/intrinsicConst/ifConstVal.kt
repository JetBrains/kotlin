// !LANGUAGE: +IntrinsicConstEvaluation
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
fun <T> T.id() = this

const val flag = <!EVALUATED("true")!>true<!>
const val value = <!EVALUATED("10")!>10<!>
const val condition = <!EVALUATED("True")!>if (flag) "True" else "Error"<!>
const val withWhen = <!EVALUATED("True")!>when (flag) { true -> "True"; else -> "Error" }<!>
const val withWhen2 = <!EVALUATED("True")!>when { flag == true -> "True"; else -> "Error" }<!>
const val withWhen3 = <!EVALUATED("1")!>when(value) { 10 -> "1"; 100 -> "2"; else -> "3" }<!>
const val multibranchIf = <!EVALUATED("3")!>if (value == 100) 1 else if (value == 1000) 2 else 3<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (condition.id() != "True") return "Fail 1"
    if (withWhen.id() != "True") return "Fail 2"
    if (withWhen2.id() != "True") return "Fail 3"
    if (withWhen3.id() != "1") return "Fail 4"
    if (multibranchIf.id() != 3) return "Fail 5"
    return "OK"
}
