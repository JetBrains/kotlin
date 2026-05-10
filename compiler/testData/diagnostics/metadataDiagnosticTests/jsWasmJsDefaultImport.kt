// METADATA_TARGET_PLATFORMS: JS, WasmJs
// OPT_IN: kotlin.js.ExperimentalJsExport

// `JsExport` should be available as a default import from `import kotlin.js.*`, as it's imported by both JS and WasmJ.
@JsExport
fun test(): String = "test"
