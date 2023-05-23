// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE, WASM
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
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
