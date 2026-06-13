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
    accept<Any>(::f1)
    accept<Any>(::f2)
    accept<Any>(Int::f3)
    accept(::f1)
    accept(::f2)
    accept(Int::f3)

    accept<Any>(::p1)
    accept<Any>(Int::p3)
    accept(::p1)
    accept(Int::p3)

    val x1 = ::f1
    val x2 = ::f2
    val x3 = Int::f3
    val x3Bound = 1::f3

    accept<() -> Unit>(::f1)
    accept<(Int) -> Unit>(::f2)
    accept<Int.() -> Unit>(Int::f3)
    accept<() -> Unit>(1::f3)

    val y1 = ::p1
    val y3 = Int::p3
    val y3Bound = 1::p3

    accept<() -> Unit>(::p1)
    accept<Int.() -> Unit>(Int::p3)
    accept<() -> Unit>(1::p3)
}

context(_: String)
fun testContextParameter() {
    accept<() -> Unit>(::f1)
    accept<(Int) -> Unit>(::f2)
    accept<Int.() -> Unit>(Int::f3)
    accept<() -> Unit>(1::f3)

    accept<() -> Unit>(::p1)
    accept<Int.() -> Unit>(Int::p3)
    accept<() -> Unit>(1::p3)
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
