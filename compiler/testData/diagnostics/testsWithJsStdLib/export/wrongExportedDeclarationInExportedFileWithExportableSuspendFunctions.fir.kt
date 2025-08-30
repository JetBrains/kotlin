// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// LANGUAGE: +JsAllowExportingSuspendFunctions
@file:JsExport
package foo

suspend fun suspendFun() { }

class WithSuspendFunctionInside {
    suspend fun suspendFun() {}
}
