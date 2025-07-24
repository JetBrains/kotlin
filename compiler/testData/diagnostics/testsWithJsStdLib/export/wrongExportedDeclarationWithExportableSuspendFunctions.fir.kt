// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTICS_MESSAGES
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// LANGUAGE: +JsAllowExportingSuspendFunctions

package foo

@JsExport
suspend fun suspendFun() { }

@JsExport
class WithSuspendFunctionInside {
    suspend fun suspendFun() {}
}
