// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

@file:JsExport

package foo

<!WRONG_EXPORTED_DECLARATION("inline function with reified type parameters")!>inline fun <reified T> inlineReifiedFun(x: Any)<!> = x is T

<!WRONG_EXPORTED_DECLARATION("suspend function")!>suspend fun suspendFun()<!> { }

<!WRONG_EXPORTED_DECLARATION("extension property")!>val String.extensionProperty<!>
    get() = this.length

annotation class <!WRONG_EXPORTED_DECLARATION("annotation class")!>AnnotationClass<!>
