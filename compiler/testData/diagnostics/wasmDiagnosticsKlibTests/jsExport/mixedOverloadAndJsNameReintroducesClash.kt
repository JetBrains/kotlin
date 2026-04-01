// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

<!EXPORTING_JS_NAME_CLASH!>@JsExport @JsName("fInt") fun foo(x: Int) = 1<!>
@JsExport @JsName("fStr") fun foo(x: String) = 2

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

<!EXPORTING_JS_NAME_CLASH!>@JsExport @JsName("fInt") fun bar() = 3<!>
