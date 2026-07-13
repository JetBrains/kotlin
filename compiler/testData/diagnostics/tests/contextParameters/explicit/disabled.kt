// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters -ExplicitContextArguments

class A
class B

context(a: A) fun needsA() { }
context(b: B) fun needsB() { }
context(a: A, b: B) fun needsBoth() { }

fun explicit() {
    <!NO_CONTEXT_ARGUMENT!>needsA<!>(<!UNSUPPORTED_FEATURE!>a = A()<!>)
    <!NO_CONTEXT_ARGUMENT!>needsB<!>(<!UNSUPPORTED_FEATURE!>b = B()<!>)
    <!NO_CONTEXT_ARGUMENT, NO_CONTEXT_ARGUMENT!>needsBoth<!>(<!UNSUPPORTED_FEATURE!>a = A()<!>, <!UNSUPPORTED_FEATURE!>b = B()<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */
