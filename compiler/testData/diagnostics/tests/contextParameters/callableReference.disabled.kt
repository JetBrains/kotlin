// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// LANGUAGE_FEATURE_TOGGLED: CallableReferencesToContextual

context(_ : String) fun f1() {}
context(_ : String) fun f2(i: Int) {}
context(_ : String) fun Int.f3() {}

context(_ : String) val p1 get() = Unit
context(_ : String) val Int.p3 get() = Unit

fun testContextualFunctionType() {
    accept<context(String) () -> Unit>(::<!NO_CONTEXT_ARGUMENT!>f1<!>)
    accept<context(String) (Int) -> Unit>(::<!NO_CONTEXT_ARGUMENT!>f2<!>)
    accept<context(String) Int.() -> Unit>(Int::<!NO_CONTEXT_ARGUMENT!>f3<!>)

    accept<context(String) () -> Unit>(::<!NO_CONTEXT_ARGUMENT!>p1<!>)
    accept<context(String) Int.() -> Unit>(Int::<!NO_CONTEXT_ARGUMENT!>p3<!>)
}

context(_: String)
fun testContextualFunctionTypeWithContext() {
    accept<context(String) () -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f1<!>)
    accept<context(String) (Int) -> Unit>(::<!INAPPLICABLE_CANDIDATE!>f2<!>)
    accept<context(String) Int.() -> Unit>(Int::<!INAPPLICABLE_CANDIDATE!>f3<!>)

    accept<context(String) () -> Unit>(::<!INAPPLICABLE_CANDIDATE!>p1<!>)
    accept<context(String) Int.() -> Unit>(Int::<!INAPPLICABLE_CANDIDATE!>p3<!>)
}

fun String.testReceiver() {
    accept<Any>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>)
    accept<Any>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>)
    accept<Any>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)
    accept(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>)
    accept(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>)
    accept(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)

    accept<Any>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p1<!>)
    accept<Any>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>)
    accept(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p1<!>)
    accept(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>)

    val x1 = ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>
    val x2 = ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>
    val x3 = Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>
    val x3Bound = 1::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>

    accept<() -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>)
    accept<(Int) -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>)
    accept<Int.() -> Unit>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)
    accept<() -> Unit>(1::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)

    val y1 = ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p1<!>
    val y3 = Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>
    val y3Bound = 1::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>

    accept<() -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p1<!>)
    accept<Int.() -> Unit>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>)
    accept<() -> Unit>(1::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>)
}

context(_: String)
fun testContextParameter() {
    accept<() -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f1<!>)
    accept<(Int) -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f2<!>)
    accept<Int.() -> Unit>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)
    accept<() -> Unit>(1::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>f3<!>)

    accept<() -> Unit>(::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p1<!>)
    accept<Int.() -> Unit>(Int::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>)
    accept<() -> Unit>(1::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>p3<!>)
}

fun testMissingContext() {
    accept<() -> Unit>(::<!NO_CONTEXT_ARGUMENT!>f1<!>)
    accept<(Int) -> Unit>(::<!NO_CONTEXT_ARGUMENT!>f2<!>)
    accept<Int.() -> Unit>(Int::<!NO_CONTEXT_ARGUMENT!>f3<!>)
    accept<() -> Unit>(1::<!NO_CONTEXT_ARGUMENT!>f3<!>)

    accept<() -> Unit>(::<!NO_CONTEXT_ARGUMENT!>p1<!>)
    accept<Int.() -> Unit>(Int::<!NO_CONTEXT_ARGUMENT!>p3<!>)
    accept<() -> Unit>(1::<!NO_CONTEXT_ARGUMENT!>p3<!>)
}

fun <T> accept(t: T) {}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
functionalType, integerLiteral, nullableType, typeParameter, typeWithContext, typeWithExtension */
