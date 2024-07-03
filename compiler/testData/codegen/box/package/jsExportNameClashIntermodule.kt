// ISSUE: KT-60832, KT-65779
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// KT-65779: SyntaxError: Identifier 'bar' has already been declared

// FILE: f1.kt
package foo
@JsExport fun bar() = "O"

// FILE: f2.kt
package baz
@JsExport fun bar() = "K"

// FILE: main.kt
fun box(): String = foo.bar() + baz.bar()
