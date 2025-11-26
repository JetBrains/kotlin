// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: -JsAllowExportingSuspendFunctions +ContextParameters
package foo

<!WRONG_EXPORTED_DECLARATION("suspend function")!>@JsExport
suspend fun suspendFun()<!> { }

@JsExport
class WithSuspendFunctionInside {
    <!WRONG_EXPORTED_DECLARATION("suspend function")!>suspend fun suspendFun()<!> {}
}

<!WRONG_EXPORTED_DECLARATION!>@JsExport
suspend fun Int.suspendExtensionFun()<!> {}

<!WRONG_EXPORTED_DECLARATION!>@JsExport
context(ctx: Int)
suspend fun suspendFunWithContext()<!> = ctx.suspendExtensionFun()

@JsExport
class WithSuspendExtensionFunAndContext {
    <!WRONG_EXPORTED_DECLARATION!>context(ctx: Int)
    suspend fun Int.suspendFun()<!> {}
}

@JsExport
class WithSuspendFunInsideInnerClass {
    inner class Inner {
        <!WRONG_EXPORTED_DECLARATION!>suspend fun suspendFun()<!> {}
    }
}

<!WRONG_EXPORTED_DECLARATION!>@JsExport
inline suspend fun inlineSuspendFun()<!> = 42.suspendExtensionFun()

<!WRONG_EXPORTED_DECLARATION!>@JsExport
inline suspend fun inlineChain()<!> = inlineSuspendFun()

@JsExport
fun suspendParameter(<!NON_EXPORTABLE_TYPE!>call: suspend () -> Unit<!>) {}
