// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// IGNORING_WASM_FOR_K2
// IGNORE_BACKEND: WASM
// FIR status: don't support legacy feature
fun box() : String = when (true) {
    ((true)) -> "OK"
    (1 == 2) -> "Not ok"
}
