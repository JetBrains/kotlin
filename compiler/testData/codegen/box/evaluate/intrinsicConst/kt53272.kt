// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR, WASM_JS
// ^^^ These tests create modules that break FIR dump
// FILE: 1.kt

const val name = E.OK.name
// STOP_EVALUATION_CHECKS
fun box(): String = name

// FILE: 2.kt

enum class E(val parent: E?) {
    X(null),
    OK(X),
}
