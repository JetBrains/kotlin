// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

class A
class B

context(a: A) fun needsA() { }
context(b: B) fun needsB() { }
context(a: A, b: B) fun needsBoth() { }

fun missing() {
    <!NO_CONTEXT_ARGUMENT("a: A")!>needsA<!>()
    <!NO_CONTEXT_ARGUMENT("b: B")!>needsB<!>()
    <!NO_CONTEXT_ARGUMENT("a: A"), NO_CONTEXT_ARGUMENT("b: B")!>needsBoth<!>()
}

fun explicit() {
    needsA(a = A())
    needsB(b = B())
    needsBoth(a = A(), b = B())
}

fun problems() {
    needsA(a = <!ARGUMENT_TYPE_MISMATCH!>B()<!>)
    <!NO_CONTEXT_ARGUMENT("a: A")!>needsA<!>(<!NAMED_PARAMETER_NOT_FOUND!>b<!> = B())
    <!NO_CONTEXT_ARGUMENT("b: B")!>needsBoth<!>(a = A())
    <!NO_CONTEXT_ARGUMENT!>needsA<!>(<!TOO_MANY_ARGUMENTS!>A()<!>)
    <!NO_CONTEXT_ARGUMENT, NO_CONTEXT_ARGUMENT!>needsBoth<!>(<!TOO_MANY_ARGUMENTS!>A()<!>, <!TOO_MANY_ARGUMENTS!>B()<!>)
    <!NO_CONTEXT_ARGUMENT!>needsBoth<!>(<!TOO_MANY_ARGUMENTS!>A()<!>, b = B())
    <!NO_CONTEXT_ARGUMENT!>needsBoth<!>(a = A(), <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>B()<!>)
}

context(a: A) fun fromContext() {
    needsA()
    <!NO_CONTEXT_ARGUMENT("b: B")!>needsB<!>()
    <!NO_CONTEXT_ARGUMENT("b: B")!>needsBoth<!>()
    needsBoth(b = B())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */
