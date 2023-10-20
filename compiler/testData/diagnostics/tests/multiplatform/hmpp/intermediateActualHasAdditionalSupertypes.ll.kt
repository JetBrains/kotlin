// LL_FIR_DIVERGENCE
// False positive reports of ARGUMENT_TYPE_MISMATCH and UNRESOLVED_REFERENCE are due to bug KT-63382.
// LL_FIR_DIVERGENCE
// ISSUE: KT-57369

// MODULE: common
// TARGET_PLATFORM: Common
interface CompletionHandler {
    fun foo()
}

expect class CompletionHandlerBase()

fun invokeOnCompletion(handler: CompletionHandler) {}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
// actual has an additional super type
actual class CompletionHandlerBase : CompletionHandler {
    override fun foo() {}
}

fun cancelFutureOnCompletionAlt(handlerBase: CompletionHandlerBase) {
    invokeOnCompletion(handlerBase)
    handlerBase.foo()
}

// MODULE: main()()(common, intermediate)
// the order of dependencies is important to reproduce KT-57369
fun cancelFutureOnCompletion(handlerBase: CompletionHandlerBase) {
    invokeOnCompletion(<!ARGUMENT_TYPE_MISMATCH!>handlerBase<!>)
    handlerBase.<!UNRESOLVED_REFERENCE!>foo<!>()
}
