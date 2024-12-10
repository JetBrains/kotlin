// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR NATIVE
// ^^^ Source code is not compiled in JS, Native.
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

inline fun foo(f: () -> Unit) {
    f()
}

// FILE: 2.kt

fun box(): String = (bar@ l@ fun(): String {
    foo { return@bar "OK" }
    return "fail"
}).let { it() }
