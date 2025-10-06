// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +JsAllowExportingSuspendFunctions +ContextParameters
package foo

@JsExport
suspend fun Int.suspendExtensionFun() {}

@JsExport
context(ctx: Int)
suspend fun suspendFunWithContext() = ctx.suspendExtensionFun()

@JsExport
class WithSuspendExtensionFunAndContext {
    context(ctx: Int)
    suspend fun Int.suspendFun() {}
}

@JsExport
class WithSuspendFunInsideInnerClass {
    inner class Inner {
        suspend fun suspendFun() {}
    }
}

@JsExport
inline suspend fun inlineSuspendFun() = 42.suspendExtensionFun()
