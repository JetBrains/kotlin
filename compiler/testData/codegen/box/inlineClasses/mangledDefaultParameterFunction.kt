// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
inline class X(val s: String)
fun foo(x: X, block: (X) -> String = { it.s }) = block(x)

fun box(): String {
    return foo(X("OK"))
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
