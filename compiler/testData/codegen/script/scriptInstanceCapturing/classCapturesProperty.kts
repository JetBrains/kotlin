// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI
// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

// expected: rv: abc

// KT-19423
val used = "abc"
class User {
    val property = used
}

val rv = User().property
