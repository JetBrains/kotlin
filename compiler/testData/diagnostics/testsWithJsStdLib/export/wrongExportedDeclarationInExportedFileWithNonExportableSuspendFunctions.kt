// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: -JsAllowExportingSuspendFunctions +ContextParameters
@file:JsExport
package foo

<!WRONG_EXPORTED_DECLARATION!>suspend fun suspendFun()<!> { }

class WithSuspendFunctionInside {
    <!WRONG_EXPORTED_DECLARATION!>suspend fun suspendFun()<!> {}
}

<!WRONG_EXPORTED_DECLARATION!>suspend fun Int.suspendExtensionFun()<!> {}

<!WRONG_EXPORTED_DECLARATION!>context(ctx: Int)
suspend fun suspendFunWithContext()<!> = ctx.suspendExtensionFun()

class WithSuspendExtensionFunAndContext {
    <!WRONG_EXPORTED_DECLARATION!>context(ctx: Int)
    suspend fun Int.suspendFun()<!> {}
}

class WithSuspendFunInsideInnerClass {
    inner class Inner {
        <!WRONG_EXPORTED_DECLARATION!>suspend fun suspendFun()<!> {}
    }
}

<!WRONG_EXPORTED_DECLARATION!>inline suspend fun inlineSuspendFun()<!> = suspendFun()

<!WRONG_EXPORTED_DECLARATION!>inline suspend fun inlineChain()<!> = inlineSuspendFun()

fun suspendParameter(<!NON_EXPORTABLE_TYPE!>call: suspend () -> Unit<!>) {}
