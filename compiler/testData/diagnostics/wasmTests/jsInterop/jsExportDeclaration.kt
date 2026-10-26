// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// OPT_IN: kotlin.wasm.ExperimentalWasmInterop
// RENDER_DIAGNOSTIC_ARGUMENTS

import kotlin.wasm.WasmImport

<!WRONG_EXPORTED_DECLARATION("external function")!>@JsExport
external fun externalFun(): Int<!>

<!WRONG_EXPORTED_DECLARATION("external function")!>@JsExport
@JsFun("() => 1")
external fun jsFunFun(): Int<!>

<!WRONG_EXPORTED_DECLARATION("external function")!>@JsExport
@WasmImport("module", "name")
external fun wasmImportFun(): Int<!>

<!WRONG_EXPORTED_DECLARATION("inline function with reified type parameters")!>@JsExport
inline fun <reified T : JsAny> inlineReifiedFun(x: JsAny): Boolean<!> = x is T

@JsExport
<!NOTHING_TO_INLINE!>inline<!> fun <T : JsAny> inlineFun(x: T): T = x

@JsExport
<!NOTHING_TO_INLINE!>inline<!> fun <T : <!WRONG_JS_INTEROP_TYPE("Any; upper bound of JS interop type parameter")!>Any<!>> inlineFun(x: T): T = x

<!WRONG_EXPORTED_DECLARATION("suspend function")!>@JsExport
suspend fun suspendFun(): Int<!> = 1

<!RESERVED_WORD_AS_JS_EXPORT_NAME("delete")!>@JsExport
fun delete(): Int = 1<!>

@JsExport
@JsName(<!RESERVED_WORD_AS_JS_EXPORT_NAME("default")!>"default"<!>)
fun defaultFun(): Int = 1

@JsExport
@JsName(<!RESERVED_WORD_AS_JS_EXPORT_NAME("this")!>"this"<!>)
fun thisFun(): Int = 1

@JsExport
fun `with space`(): Int = 1

// names used by Kotlin/Wasm for its own exports

<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME("memory")!>@JsExport
fun memory(): Int = 1<!>

<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME("__callFunction_foo")!>@JsExport
fun __callFunction_foo(): Int = 1<!>

@JsExport
@JsName(<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME("__vt$foo")!>"__vt\$foo"<!>)
fun vtFoo(): Int = 1

@JsExport
@JsName(<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME("__it$foo")!>"__it\$foo"<!>)
fun itFoo(): Int = 1

@JsExport
@JsName(<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME("__rt$foo")!>"__rt\$foo"<!>)
fun rtFoo(): Int = 1

@JsExport
@JsName(<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME("__fn$foo")!>"__fn\$foo"<!>)
fun fnFoo(): Int = 1

@JsExport
@JsName(<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME("memory")!>"memory"<!>)
fun memoryWithJsName(): Int = 1

<!WRONG_EXPORTED_DECLARATION("extension function")!>@JsExport
fun Int.extensionFun(): Int<!> = this

<!WRONG_EXPORTED_DECLARATION("function with vararg parameter")!>@JsExport
fun varargFun(vararg x: Int): Int<!> = x.size

<!WRONG_EXPORTED_DECLARATION("function with vararg parameter")!>@JsExport
fun varargStringFun(prefix: String, vararg x: String): String<!> = prefix + x.joinToString()

<!WRONG_EXPORTED_DECLARATION("function with vararg parameter")!>@JsExport
fun varargInTheMiddleFun(x: Int, vararg a: Int, y: Int): Int<!> = x + a.sum() + y

@JsExport
fun arrayParameterFun(x: JsArray<JsNumber>): Int = x.length
