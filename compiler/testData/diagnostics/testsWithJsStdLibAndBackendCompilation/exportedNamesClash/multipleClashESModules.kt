// FIR_IDENTICAL
// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT
// FILE: Function1.kt
@file:Suppress("OPT_IN_USAGE")
package Function1

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport fun test() = 1<!>

// FILE: Function2.kt
@file:Suppress("OPT_IN_USAGE")
package Function2

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport fun test() = 1<!>

// FILE: Function3.kt
@file:Suppress("OPT_IN_USAGE")

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport fun test() = 1<!>

// FILE: Property1.kt
@file:Suppress("OPT_IN_USAGE")
package Property1

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport val test = 1<!>

// FILE: Property2.kt
@file:Suppress("OPT_IN_USAGE")
package Property2

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport val test = 1<!>

// FILE: Class1.kt
@file:Suppress("OPT_IN_USAGE")
package Class1

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport class test<!>

// FILE: Class2.kt
@file:Suppress("OPT_IN_USAGE")
package Class2

<!EXPORTING_JS_NAME_CLASH_ES!>@JsExport class test<!>
