// IGNORE_BACKEND: WASM, JS_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: result.kt

package kotlin

inline class Result(val value: Any?)

// FILE: box.kt

fun box(): String {
    return Result("OK").value as String
}
