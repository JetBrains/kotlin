// OPT_IN: kotlin.js.ExperimentalJsExport
// MODULE: m1
// FILE: f1.kt
package foo

<!JS_EXPORT_CLASH!>@JsExport fun exportedFun() = "Hello"<!>

// FILE: f2.kt
package foo.bar

<!JS_EXPORT_CLASH!>@JsExport fun exportedFun() = "World"<!>

// MODULE: m2(m1)
// FILE: f1.kt
package baz

@JsExport fun exportedFun() = "Hello"
