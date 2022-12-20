// !OPT_IN: kotlin.js.ExperimentalJsExport

// MODULE: m1
// FILE: a.kt
package foo

<!EXPORTED_NAME_CLASH!>@JsExport
class A<!>

<!EXPORTED_NAME_CLASH!>@JsExport
class B<!>

@JsExport
class C

<!EXPORTED_NAME_CLASH!>@JsExport
@JsName("SameJsName")
class E<!>

// FILE: b.kt

<!EXPORTED_NAME_CLASH!>@JsExport
class A<!>

<!EXPORTED_NAME_CLASH!>@JsExport
@JsName("B")
class C<!>

@JsExport
class D

<!EXPORTED_NAME_CLASH!>@JsExport
@JsName("SameJsName")
class F<!>

// MODULE: m2(m1)
// FILE: c.kt
@JsExport
class C

@JsExport
class D

@JsExport
@JsName("SameJsName")
class E
