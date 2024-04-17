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
    companion object {
        fun foo() = 42
    }
}

@JsExport
interface InterfaceWithNamedCompanion {
    companion <!NAMED_COMPANION_IN_EXPORTED_INTERFACE!>object Named<!> {
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
inline class <!WRONG_EXPORTED_DECLARATION("inline class")!>B(val b: Int)<!>

@JsExport
inline value class <!WRONG_EXPORTED_DECLARATION("inline value class")!>C(val c: Int)<!>

@JsExport
value inline class <!WRONG_EXPORTED_DECLARATION("inline value class")!>D(val d: Int)<!>

@JsExport
external interface ExternalInterface

@JsExport
external enum class <!ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING!>ExternalEnum<!> {
    A
}

@JsExport
external object ExternalObject {
    object NestedObject
}

@JsExport
external class ExternalClass {
    class NestedClass
}

@JsExport
external fun baz(): String

@JsExport
external var qux: String

external var quux: String
    <!WRONG_ANNOTATION_TARGET("getter")!>@JsExport<!>
    get() = definedExternally
    <!WRONG_ANNOTATION_TARGET("setter")!>@JsExport<!>
    set(v) = definedExternally

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:JsExport<!>
<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@set:JsExport<!>
external var quuux: String

<!WRONG_ANNOTATION_TARGET("typealias")!>@JsExport<!>
<!WRONG_MODIFIER_TARGET("external; typealias")!>external<!> typealias ExternalTypeAlias = String
