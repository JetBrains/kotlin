// IGNORE_BACKEND: WASM, JS_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_BACKEND: NATIVE
// ALLOW_KOTLIN_PACKAGE
// FILE: result.kt
package kotlin

inline class Result(val value: Any?)

// FILE: box.kt

fun box(): String {
    return Result("OK").value as String
}
