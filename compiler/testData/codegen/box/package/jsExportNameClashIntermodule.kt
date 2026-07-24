// ISSUE: KT-60832, KT-65779
// DONT_TARGET_EXACT_BACKEND: JVM, JVM_IR, NATIVE, WASM_WASI
// IGNORE_BACKEND: WASM_JS
// KT-65779: SyntaxError: Identifier 'bar' has already been declared

// FILE: f1.kt
package foo
@JsExport fun bar() = "O"

// FILE: f2.kt
package baz
@JsExport fun bar() = "K"

// FILE: main.kt
fun box(): String = foo.bar() + baz.bar()
