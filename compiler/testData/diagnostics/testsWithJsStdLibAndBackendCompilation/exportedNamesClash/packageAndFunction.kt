// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo.bar

@JsExport
val test = 1

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun bar() = 2<!>

// FILE: C.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun foo() = 2<!>
