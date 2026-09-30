// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
package foo

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("bar")
external var bar: Int<!> = definedExternally

typealias JsM = JsModule

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsM("bar")
external var bar2: Int<!> = definedExternally

// A qualified declaration is referenced through its qualifier, so writing to it does not reach the module.
@JsModule("bar")
@JsQualifier("a.b")
external var qualifiedBar: Int = definedExternally
