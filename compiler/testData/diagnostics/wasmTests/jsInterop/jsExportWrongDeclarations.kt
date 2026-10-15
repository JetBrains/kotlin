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

<!WRONG_EXPORTED_DECLARATION("suspend function")!>@JsExport
suspend fun suspendFun(): Int<!> = 1

<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("delete")!>@JsExport
fun delete(): Int = 1<!>

@JsExport
@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("default")!>"default"<!>)
fun defaultFun(): Int = 1

@JsExport
@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("this")!>"this"<!>)
fun thisFun(): Int = 1

@JsExport
fun `with space`(): Int = 1

@JsExport
fun Int.extensionFun(): Int = this
