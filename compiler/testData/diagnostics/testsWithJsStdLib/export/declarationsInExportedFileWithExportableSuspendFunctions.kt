// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +JsAllowExportingSuspendFunctions +ContextParameters
@file:JsExport
package foo

suspend fun Int.suspendExtensionFun() {}

context(ctx: Int)
suspend fun suspendFunWithContext() = ctx.suspendExtensionFun()

class WithSuspendExtensionFunAndContext {
    context(ctx: Int)
    suspend fun Int.suspendFun() {}
}

class WithSuspendFunInsideInnerClass {
    inner class Inner {
        suspend fun suspendFun() {}
    }
}

inline suspend fun inlineSuspendFun() = 42.suspendExtensionFun()
