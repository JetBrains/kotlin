// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

@file:JsExport

package foo

@JsExport.Ignore
inline fun <reified T> inlineReifiedFun(x: Any) = x is T

@JsExport.Ignore
suspend fun suspendFun() { }

@JsExport.Ignore
val String.extensionProperty get() = this.length

@JsExport.Ignore
val Array<*>.extensionProperty get() = this.size

@JsExport.Ignore
annotation class AnnotationClass

class AnotherClass {
    @JsExport.Ignore
    val String.extensionProperty get() = this.length
    @JsExport.Ignore
    val Array<*>.extensionProperty get() = this.size
}
