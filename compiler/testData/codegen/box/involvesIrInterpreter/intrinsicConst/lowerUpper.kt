// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
fun <T> T.id() = this

const val lower1 = "hEllO".<!EVALUATED("hello")!>lowercase()<!>
const val upper1 = "World".<!EVALUATED("WORLD")!>uppercase()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (lower1.id() != "hello") return "Fail lower1"
    if (upper1.id() != "WORLD") return "Fail upper1"
    return "OK"
}
