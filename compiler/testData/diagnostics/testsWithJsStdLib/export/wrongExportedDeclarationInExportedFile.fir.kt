// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

@file:JsExport

package foo

inline fun <reified T> inlineReifiedFun(x: Any) = x is T

suspend fun suspendFun() { }

val String.extensionProperty
    get() = this.length

annotation class AnnotationClass
