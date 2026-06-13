// METADATA_TARGET_PLATFORMS: JS, WasmWasi
// OPT_IN: kotlin.js.ExperimentalJsExport

// `JsExport` should NOT be available as a default import because WasmWasi doesn't import `kotlin.js.*`.
@<!UNRESOLVED_REFERENCE!>JsExport<!>
fun test(): String = "test"
