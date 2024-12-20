// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM, NATIVE
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR, NATIVE
// ISSUE: KT-74045: CONFLICTING_KLIB_SIGNATURES_ERROR

class A

context(a: A)
val b: String
    get() = "O"

val b: String
    get() = "K"

fun o() = with(A()) { b }
fun k() = b

fun box() = o() + k()
