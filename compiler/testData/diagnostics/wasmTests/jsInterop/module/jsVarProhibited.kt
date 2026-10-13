// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// RENDER_DIAGNOSTICS_FULL_TEXT
package foo

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("bar")
external var bar: Int<!> = definedExternally

typealias JsM = JsModule

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsM("bar")
external var bar2: Int<!> = definedExternally

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("bar")
@JsQualifier("a.b")
external var qualifiedBar: Int<!> = definedExternally
