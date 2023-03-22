// ISSUE: KT-57369

// MODULE: common
// TARGET_PLATFORM: Common
interface CompletionHandler {
    fun foo()
}

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>class CompletionHandlerBase<!>()

fun invokeOnCompletion(handler: CompletionHandler) {}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
// actual has an additional super type
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class CompletionHandlerBase<!> : CompletionHandler {
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
