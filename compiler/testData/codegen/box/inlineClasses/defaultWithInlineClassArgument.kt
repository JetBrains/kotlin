// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: a.kt
fun box() = A(0).f()

// FILE: b.kt
inline class A(val i: Int)

fun A.f(xs: Array<String> = Array<String>(1) { "OK" }) = xs[i]

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
