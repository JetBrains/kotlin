// LANGUAGE: +ExplicitBackingFields
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

val field: String = "OK"

val a: Any
    field = field

fun box(): String {
    return a
}