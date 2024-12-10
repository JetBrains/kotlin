// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// FIR status: don't support legacy feature
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.
fun box() : String = when (true) {
    ((true)) -> "OK"
    (1 == 2) -> "Not ok"
}
