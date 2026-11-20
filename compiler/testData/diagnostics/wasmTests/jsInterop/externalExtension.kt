// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// OPT_IN: kotlin.wasm.ExperimentalWasmInterop
// RENDER_DIAGNOSTIC_ARGUMENTS

import kotlin.wasm.WasmImport

external interface I : JsAny

// top-level extensions

<!WRONG_EXTERNAL_DECLARATION("extension function")!>external fun Int.extensionFun(): Int<!>

<!WRONG_EXTERNAL_DECLARATION("extension function")!>external fun I.extensionFunWithExternalReceiver(): Int<!>

<!WRONG_EXTERNAL_DECLARATION("extension property")!>external val Int.extensionVal: Int<!>

<!WRONG_EXTERNAL_DECLARATION("extension property")!>external var I.extensionVar: Int<!>

<!WRONG_EXTERNAL_DECLARATION("extension function")!>@JsFun("(x) => x")
external fun Int.jsFunExtensionFun(): Int<!>

<!WRONG_EXTERNAL_DECLARATION("extension function")!>@WasmImport("module", "name")
external fun Int.wasmImportExtensionFun(): Int<!>

// extension members of external classes and interfaces

external class C {
    <!WRONG_EXTERNAL_DECLARATION("extension function")!>fun Int.memberExtensionFun(): Int<!>
    <!WRONG_EXTERNAL_DECLARATION("extension property")!>val Int.memberExtensionVal: Int<!>
}

external interface J {
    <!WRONG_EXTERNAL_DECLARATION("extension function")!>fun Int.memberExtensionFun(): Int<!>
    <!WRONG_EXTERNAL_DECLARATION("extension property")!>val Int.memberExtensionVal: Int<!>
}
