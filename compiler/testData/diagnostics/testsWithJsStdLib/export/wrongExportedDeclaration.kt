// !USE_EXPERIMENTAL: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

<!WRONG_EXPORTED_DECLARATION("inline function with reified type parameters")!>@JsExport
inline fun <reified T> inlineReifiedFun(x: Any)<!> = x is T

<!WRONG_EXPORTED_DECLARATION("suspend function")!>@JsExport
suspend fun suspendFun()<!> { }

<!WRONG_EXPORTED_DECLARATION("extension property")!>@JsExport
val String.extensionProperty<!>
    get() = this.length

@JsExport
enum class <!WRONG_EXPORTED_DECLARATION("enum class")!>EnumClass<!> { ENTRY1, ENTRY2 }

@JsExport
annotation class <!WRONG_EXPORTED_DECLARATION("annotation class")!>AnnotationClass<!>

@JsExport
interface <!WRONG_EXPORTED_DECLARATION("interface")!>SomeInterface<!>

@JsExport
external interface GoodInterface
