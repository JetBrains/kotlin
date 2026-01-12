// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun foo() = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

<!EXPORTING_JS_NAME_CLASH!>@JsExport @JsName("foo") fun bar() = 2<!>

// FILE: C.kt
@file:Suppress("OPT_IN_USAGE")
package c

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun foo() = 3<!>
