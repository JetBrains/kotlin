// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6

// FILE: 1.kt

const val name = E.OK.<!EVALUATED("OK")!>name<!>
// STOP_EVALUATION_CHECKS
fun box(): String = name

// FILE: 2.kt

enum class E(val parent: E?) {
    X(null),
    OK(X),
}
