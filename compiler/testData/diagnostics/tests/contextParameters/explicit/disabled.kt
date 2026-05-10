// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters -ExplicitContextArguments

class A
class B

context(a: A) fun needsA() { }
context(b: B) fun needsB() { }
context(a: A, b: B) fun needsBoth() { }

fun explicit() {
    <!NO_CONTEXT_ARGUMENT!>needsA<!>(<!NAMED_PARAMETER_NOT_FOUND!>a<!> = A())
    <!NO_CONTEXT_ARGUMENT!>needsB<!>(<!NAMED_PARAMETER_NOT_FOUND!>b<!> = B())
    <!NO_CONTEXT_ARGUMENT, NO_CONTEXT_ARGUMENT!>needsBoth<!>(<!NAMED_PARAMETER_NOT_FOUND!>a<!> = A(), <!NAMED_PARAMETER_NOT_FOUND!>b<!> = B())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */
