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
interface SomeInterface

@JsExport
external interface GoodInterface

@JsExport
interface InterfaceWithCompanion {
    companion <!WRONG_EXPORTED_DECLARATION("companion object inside exported interface")!>object<!> {
        fun foo() = 42
    }
}

@JsExport
interface OuterInterface {
    class <!WRONG_EXPORTED_DECLARATION("nested class inside exported interface")!>Nested<!>
}

@JsExport
value class <!WRONG_EXPORTED_DECLARATION("value class")!>A(val a: Int)<!>

@JsExport
inline class <!WRONG_EXPORTED_DECLARATION("value class")!>B(val b: Int)<!>

@JsExport
inline value class <!WRONG_EXPORTED_DECLARATION("value class")!>C(val c: Int)<!>

@JsExport
value inline class <!WRONG_EXPORTED_DECLARATION("value class")!>D(val d: Int)<!>
