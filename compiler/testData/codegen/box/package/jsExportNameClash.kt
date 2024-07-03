// ISSUE: KT-60832, KT-65779
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
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
