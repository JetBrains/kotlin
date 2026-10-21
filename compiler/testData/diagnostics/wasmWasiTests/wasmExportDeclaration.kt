// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

@file:OptIn(ExperimentalWasmInterop::class)

import kotlin.wasm.WasmExport
import kotlin.wasm.ExperimentalWasmInterop

// names used by Kotlin/Wasm for its own exports

@WasmExport("memory")
fun exportedAsMemory(): Int = 1

@WasmExport("__callFunction_foo")
fun exportedAsCallFunction(): Int = 1

@WasmExport("__vt\$foo")
fun exportedAsVtable(): Int = 1

@WasmExport("_initialize")
fun exportedAsInitialize(): Int = 1

@WasmExport("_start")
fun exportedAsStart(): Int = 1

@WasmExport("startUnitTests")
fun exportedAsStartUnitTests(): Int = 1

// JS reserved words

@WasmExport("delete")
fun exportedAsDelete(): Int = 1

@WasmExport("default")
fun exportedAsDefault(): Int = 1

@WasmExport
fun `class`(): Int = 1

// names that are not valid JS identifiers are exported to JS as string literals

@WasmExport("with space")
fun exportedWithSpace(): Int = 1

@WasmExport("")
fun exportedWithEmptyName(): Int = 1

// function kinds

<!WASM_EXPORT_UNSUPPORTED_FUNCTION_KIND("extension function")!>@WasmExport
fun Int.extensionWithSupportedReceiver(): Int<!> = this

<!WASM_EXPORT_UNSUPPORTED_FUNCTION_KIND("extension function")!>@WasmExport
fun String.extensionWithUnsupportedReceiver(): Int<!> = length

<!WASM_EXPORT_UNSUPPORTED_FUNCTION_KIND("suspend function")!>@WasmExport
suspend fun suspendFunction(): Int<!> = 1

@WasmExport
<!NOTHING_TO_INLINE!>inline<!> fun inlineFunction(): Int = 1

<!WASM_EXPORT_UNSUPPORTED_FUNCTION_KIND("inline function with reified type parameters")!>@WasmExport
inline fun <reified T> inlineReifiedFunction(): Int<!> = 1
