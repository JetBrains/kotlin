// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package foo

<!WRONG_EXPORTED_DECLARATION("inline function with reified type parameters")!>@JsExport
inline fun <reified T> inlineReifiedFun(x: Any)<!> = x is T

<!WRONG_EXPORTED_DECLARATION("suspend function")!>@JsExport
suspend fun suspendFun()<!> { }

<!WRONG_EXPORTED_DECLARATION("extension property")!>@JsExport
val String.extensionProperty<!>
    get() = this.length

@JsExport
annotation class <!WRONG_EXPORTED_DECLARATION("annotation class")!>AnnotationClass<!>

@JsExport
interface <!WRONG_EXPORTED_DECLARATION("interface")!>SomeInterface<!>

@JsExport
external interface GoodInterface

@JsExport
value class <!WRONG_EXPORTED_DECLARATION("value class")!>A(val a: Int)<!>

@JsExport
inline class <!WRONG_EXPORTED_DECLARATION("inline class")!>B(val b: Int)<!>

@JsExport
inline value class <!WRONG_EXPORTED_DECLARATION("inline value class")!>C(val c: Int)<!>

@JsExport
value inline class <!WRONG_EXPORTED_DECLARATION("inline value class")!>D(val d: Int)<!>
