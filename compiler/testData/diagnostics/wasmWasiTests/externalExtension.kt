// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

@file:OptIn(ExperimentalWasmInterop::class)

import kotlin.wasm.WasmImport

<!WRONG_EXTERNAL_DECLARATION("extension function")!>@WasmImport("module", "name")
external fun Int.wasmImportExtensionFun(): Int<!>

<!WRONG_EXTERNAL_DECLARATION("extension function")!>@WasmImport("module", "name")
external fun Long.wasmImportExtensionFunWithSupportedReceiver(x: Int): Int<!>

<!WRONG_EXTERNAL_DECLARATION("extension function")!>@WasmImport("module", "name")
external fun String.wasmImportExtensionFunWithUnsupportedReceiver(): Int<!>
