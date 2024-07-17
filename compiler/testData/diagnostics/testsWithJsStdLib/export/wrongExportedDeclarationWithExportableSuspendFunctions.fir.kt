// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTICS_MESSAGES
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// LANGUAGE: +JsExportSuspendFunctions

package foo

@JsExport
suspend fun suspendFun() { }

@JsExport
class WithSuspendFunctionInside {
    suspend fun suspendFun() {}
}
