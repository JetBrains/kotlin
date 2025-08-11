// FIR_IDENTICAL
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FILE: Function1.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun test() = 1<!>

// FILE: Function2.kt
@file:Suppress("OPT_IN_USAGE")

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun foo() = 1<!>

// FILE: Property1.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz

<!EXPORTING_JS_NAME_CLASH!>@JsExport @JsName("test") fun bar() = 2<!>

// FILE: Property2.kt
@file:Suppress("OPT_IN_USAGE")
package foo

@JsExport fun bar() = 2

// FILE: Property3.kt
@file:Suppress("OPT_IN_USAGE")

<!EXPORTING_JS_NAME_CLASH!>@JsExport @JsName("foo") fun bar() = 3<!>

// FILE: Package1.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz.test

@JsExport fun foo1() = 1

// FILE: Package2.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar.baz.test.a.b.c

@JsExport fun foo2() = 1
