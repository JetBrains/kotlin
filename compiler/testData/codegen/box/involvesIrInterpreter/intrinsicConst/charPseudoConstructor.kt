// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
fun <T> T.id() = this

const val char1 = <!EVALUATED("A")!>Char(65)<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (char1.id() != 'A') return "Fail char1"
    return "OK"
}
