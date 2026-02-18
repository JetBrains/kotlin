// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// LANGUAGE: +AllowInterfaceNestedClassesInJsExport +AllowNamedCompanionForJsExport

package foo

<!WRONG_EXPORTED_DECLARATION("inline function with reified type parameters")!>@JsExport
inline fun <reified T> inlineReifiedFun(x: Any)<!> = x is T

@JsExport
suspend fun suspendFun() { }

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
    companion object Named {
        fun foo() = 42
    }
}

@JsExport
interface OuterInterface {
    class Nested
}

@JsExport
value class <!WRONG_EXPORTED_DECLARATION("value class")!>A(val a: Int)<!>

@JsExport
inline class <!WRONG_EXPORTED_DECLARATION("value class")!>B(val b: Int)<!>

@JsExport
<!INCOMPATIBLE_MODIFIERS("inline; value")!>inline<!> <!INCOMPATIBLE_MODIFIERS("value; inline")!>value<!> class <!WRONG_EXPORTED_DECLARATION("value class")!>C(val c: Int)<!>

<!MULTIPLE_JS_EXPORT_DEFAULT_IN_ONE_FILE!>@JsExport.Default
<!INCOMPATIBLE_MODIFIERS("value; inline")!>value<!> <!INCOMPATIBLE_MODIFIERS("inline; value")!>inline<!> class <!WRONG_EXPORTED_DECLARATION("value class")!>D(val d: Int)<!><!>

@JsExport
external interface ExternalInterface

@JsExport
external interface ExternalInterfaceWithCompanion {
    companion <!WRONG_EXPORTED_DECLARATION("external companion object")!>object<!> {
        fun foo(): String
    }
}

<!MULTIPLE_JS_EXPORT_DEFAULT_IN_ONE_FILE!>@JsExport.Default
external interface DefaultExternalInterface<!>

@JsExport
external enum class <!ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING, WRONG_EXPORTED_DECLARATION("external enum class")!>ExternalEnum<!> {
    A
}

@JsExport
external <!WRONG_EXPORTED_DECLARATION("external object")!>object ExternalObject<!> {
    object NestedObject
}

@JsExport
external class <!WRONG_EXPORTED_DECLARATION("external class")!>ExternalClass<!> {
    class NestedClass
}

<!WRONG_EXPORTED_DECLARATION("external function")!>@JsExport
external fun baz(): String<!>

<!WRONG_EXPORTED_DECLARATION("external property")!>@JsExport
external var qux: String<!>

external var quux: String
    <!NESTED_JS_EXPORT, WRONG_ANNOTATION_TARGET("getter; class, property, function, file")!>@JsExport<!>
    get() = definedExternally
    <!NESTED_JS_EXPORT, WRONG_ANNOTATION_TARGET("setter; class, property, function, file")!>@JsExport<!>
    set(v) = definedExternally

<!NESTED_JS_EXPORT, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET("getter; get; class, property, function, file")!>@get:JsExport<!>
<!NESTED_JS_EXPORT, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET("setter; set; class, property, function, file")!>@set:JsExport<!>
external var quuux: String

<!WRONG_EXPORTED_DECLARATION("typealias")!><!WRONG_ANNOTATION_TARGET("typealias; class, property, function, file")!>@JsExport<!>
<!WRONG_MODIFIER_TARGET("external; typealias")!>external<!> typealias ExternalTypeAlias = String<!>
