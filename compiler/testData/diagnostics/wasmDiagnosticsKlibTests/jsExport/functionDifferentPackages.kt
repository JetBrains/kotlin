// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package foo

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun test() = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar

<!EXPORTING_JS_NAME_CLASH!>@JsExport @JsName("test") fun bar() = 2<!>
