// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// RENDER_DIAGNOSTIC_ARGUMENTS

@file:OptIn(ExperimentalWasmInterop::class)

import kotlin.wasm.WasmExport
import kotlin.wasm.ExperimentalWasmInterop

class Carrier {
    companion {
        val base = 1
    }
}

@WasmExport
companion fun Carrier.companionExtensionFun(value: Int): Int = Carrier.base + value

<!WASM_EXPORT_UNSUPPORTED_FUNCTION_KIND("extension function")!>@WasmExport
fun Carrier.instanceExtensionFun(value: Int): Int<!> = Carrier.base + value
