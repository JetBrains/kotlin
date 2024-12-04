// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package foo

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport val test = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport @JsName("test") val bar = 2<!>
