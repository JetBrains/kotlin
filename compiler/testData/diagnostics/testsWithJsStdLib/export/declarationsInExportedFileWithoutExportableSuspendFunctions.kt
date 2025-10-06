// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: -JsAllowExportingSuspendFunctions +ContextParameters
@file:JsExport
package foo

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

<!WRONG_EXPORTED_DECLARATION!>inline suspend fun inlineSuspendFun()<!> = 42.suspendExtensionFun()
