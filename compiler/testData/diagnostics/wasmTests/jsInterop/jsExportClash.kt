// OPT_IN: kotlin.js.ExperimentalJsExport
// MODULE: m1
// FILE: f1.kt
package foo

@JsExport fun exportedFun() = "Hello"

// FILE: f2.kt
package foo.bar

@JsExport fun exportedFun() = "World"

// MODULE: m2(m1)
// FILE: f1.kt
package baz

@JsExport fun exportedFun() = "Hello"
