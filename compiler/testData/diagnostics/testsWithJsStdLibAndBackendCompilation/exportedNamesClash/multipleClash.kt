// FIR_IDENTICAL
// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT
// FILE: Function1.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo.bar.baz

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport fun test() = 1<!>

// FILE: Function2.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo.bar

<!EXPORTING_JS_NAME_CLASH!>@JsExport fun baz() = 1<!>

// FILE: Function3.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport fun foo() = 1<!>

// FILE: Property1.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo.bar.baz

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport @JsName("test") val bar = 2<!>

// FILE: Property2.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport val bar = 2<!>

// FILE: Property3.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport @JsName("foo")  val bar = 3<!>

// FILE: Class1.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo.bar.baz

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport @JsName("test") class Class1<!>

// FILE: Class2.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo

<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport @JsName("bar") class Class2<!>

// FILE: Class3.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
<!EXPORTING_JS_NAME_CLASH, EXPORTING_JS_NAME_CLASH_ES!>@JsExport @JsName("foo") class Class3<!>

// FILE: Package1.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo.bar.baz.test

@JsExport val foo1 = 1

// FILE: Package2.kt
@file:Suppress("OPT_IN_USAGE", "JS_NAME_CLASH")
package foo.bar.baz.test.a.b.c

@JsExport val foo2 = 1
