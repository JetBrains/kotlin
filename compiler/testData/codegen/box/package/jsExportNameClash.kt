// ISSUE: KT-60832, KT-65779
// DONT_TARGET_EXACT_BACKEND: JVM, JVM_IR, NATIVE, WASM_WASI
// WASM_IGNORE_FOR: mode=regular
// In both single-module and multi-module Wasm mode, the different modules individually don't have conflicting exports, so the test only fails for monolith
// KT-65779: SyntaxError: Identifier 'bar' has already been declared

// MODULE: m1
// FILE: f1.kt
package foo
@JsExport fun bar() = "O"

// MODULE: m2
// FILE: f2.kt
package baz
@JsExport fun bar() = "K"

// MODULE: main(m1, m2)
// FILE: main.kt
fun box(): String = foo.bar() + baz.bar()
