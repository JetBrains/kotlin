// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package foo

@JsExport
inline fun <reified T> inlineReifiedFun(x: Any) = x is T

@JsExport
suspend fun suspendFun() { }

@JsExport
val String.extensionProperty
    get() = this.length

@JsExport
annotation class AnnotationClass

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
interface OuterInterface {
    class Nested
}

@JsExport
value class A(val a: Int)

@JsExport
inline class B(val b: Int)

@JsExport
inline value class C(val c: Int)

@JsExport
value inline class D(val d: Int)
