// LANGUAGE: -ProhibitIllegalValueParameterUsageInDefaultArguments
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// FIR status: don't support legacy feature. UNINITIALIZED_PARAMETER y. See KT-49800
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:1.9
tailrec fun foo(x: () -> String? = { y }, y: String = "fail"): String? {
    if (y == "start")
        return foo()
    return x()
}

fun box() = foo(y = "start") ?: "OK"
