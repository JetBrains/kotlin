// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(_ : String) fun f1() {}
context(_ : String) fun f2(i: Int) {}
context(_ : String) fun Int.f3() {}

fun simple() {
    accept<Any>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>)
    accept<Any>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>)
    accept<Any>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)
    accept(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>)
    accept(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>)
    accept(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)
}

fun testFunctionTypeWithContext() {
    accept<context(String) () -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>)
    accept<context(String) (Int) -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>)
    accept<context(String) Int.() -> Unit>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)
}

fun String.testReceiver() {
    accept<() -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f1<!>)
    accept<(Int) -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f2<!>)
    accept<Int.() -> Unit>(Int::<!INAPPLICABLE_CANDIDATE!>f3<!>)
    accept<() -> Unit>(1::<!INAPPLICABLE_CANDIDATE!>f3<!>)
}

context(_: String)
fun testContextParameter() {
    accept<() -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f1<!>)
    accept<(Int) -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f2<!>)
    accept<Int.() -> Unit>(Int::<!INAPPLICABLE_CANDIDATE!>f3<!>)
    accept<() -> Unit>(1::<!INAPPLICABLE_CANDIDATE!>f3<!>)
}

fun testMissingContext() {
    accept<() -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f1<!>)
    accept<(Int) -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f2<!>)
    accept<Int.() -> Unit>(Int::<!INAPPLICABLE_CANDIDATE!>f3<!>)
    accept<() -> Unit>(1::<!INAPPLICABLE_CANDIDATE!>f3<!>)
}

fun <T> accept(t: T) {}