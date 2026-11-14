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
    accept<Any>(::<!UNSUPPORTED_FEATURE("The feature \"callable references to contextual\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcallable-references-to-contextual', but note that no stability guarantees are provided.")!>f1<!>)
    accept<Any>(::<!UNSUPPORTED_FEATURE!>f2<!>)
    accept<Any>(Int::<!UNSUPPORTED_FEATURE!>f3<!>)
    accept(::<!UNSUPPORTED_FEATURE!>f1<!>)
    accept(::<!UNSUPPORTED_FEATURE!>f2<!>)
    accept(Int::<!UNSUPPORTED_FEATURE!>f3<!>)

    accept<Any>(::<!UNSUPPORTED_FEATURE!>p1<!>)
    accept<Any>(Int::<!UNSUPPORTED_FEATURE!>p3<!>)
    accept(::<!UNSUPPORTED_FEATURE!>p1<!>)
    accept(Int::<!UNSUPPORTED_FEATURE!>p3<!>)

    val x1 = ::<!UNSUPPORTED_FEATURE!>f1<!>
    val x2 = ::<!UNSUPPORTED_FEATURE!>f2<!>
    val x3 = Int::<!UNSUPPORTED_FEATURE!>f3<!>
    val x3Bound = 1::<!UNSUPPORTED_FEATURE!>f3<!>

    accept<() -> Unit>(::<!UNSUPPORTED_FEATURE!>f1<!>)
    accept<(Int) -> Unit>(::<!UNSUPPORTED_FEATURE!>f2<!>)
    accept<Int.() -> Unit>(Int::<!UNSUPPORTED_FEATURE!>f3<!>)
    accept<() -> Unit>(1::<!UNSUPPORTED_FEATURE!>f3<!>)

    val y1 = ::<!UNSUPPORTED_FEATURE!>p1<!>
    val y3 = Int::<!UNSUPPORTED_FEATURE!>p3<!>
    val y3Bound = 1::<!UNSUPPORTED_FEATURE!>p3<!>

    accept<() -> Unit>(::<!UNSUPPORTED_FEATURE!>p1<!>)
    accept<Int.() -> Unit>(Int::<!UNSUPPORTED_FEATURE!>p3<!>)
    accept<() -> Unit>(1::<!UNSUPPORTED_FEATURE!>p3<!>)
}

context(_: String)
fun testContextParameter() {
    accept<() -> Unit>(::<!UNSUPPORTED_FEATURE!>f1<!>)
    accept<(Int) -> Unit>(::<!UNSUPPORTED_FEATURE!>f2<!>)
    accept<Int.() -> Unit>(Int::<!UNSUPPORTED_FEATURE!>f3<!>)
    accept<() -> Unit>(1::<!UNSUPPORTED_FEATURE!>f3<!>)

    accept<() -> Unit>(::<!UNSUPPORTED_FEATURE!>p1<!>)
    accept<Int.() -> Unit>(Int::<!UNSUPPORTED_FEATURE!>p3<!>)
    accept<() -> Unit>(1::<!UNSUPPORTED_FEATURE!>p3<!>)
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
