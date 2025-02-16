// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57369

// MODULE: common
interface CompletionHandler {
    fun foo()
}

expect class CompletionHandlerBase()

fun invokeOnCompletion(handler: CompletionHandler) {}

// MODULE: intermediate()()(common)
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
    invokeOnCompletion(handlerBase)
    handlerBase.foo()
}
