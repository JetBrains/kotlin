// FIR_IDENTICAL
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport val test = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport @JsName("test") val bar = 2<!>
