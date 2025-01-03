// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57369

// MODULE: common
interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>CompletionHandler<!> {
    fun foo()
}

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>CompletionHandlerBase<!><!>()

<!CONFLICTING_OVERLOADS!>fun invokeOnCompletion(handler: CompletionHandler)<!> {}

// MODULE: intermediate()()(common)
// actual has an additional super type
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>CompletionHandlerBase<!><!> : CompletionHandler {
    override fun foo() {}
}

<!CONFLICTING_OVERLOADS!>fun cancelFutureOnCompletionAlt(handlerBase: CompletionHandlerBase)<!> {
    invokeOnCompletion(handlerBase)
    handlerBase.foo()
}

// MODULE: main()()(common, intermediate)
// the order of dependencies is important to reproduce KT-57369
fun cancelFutureOnCompletion(handlerBase: CompletionHandlerBase) {
    invokeOnCompletion(handlerBase)
    handlerBase.foo()
}
