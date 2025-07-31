// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// FIR_DUMP
// WITH_STDLIB

class A
class B

context(a: A)
fun foo(): Int = 4

fun runFoo() {
    inject val a = A()
    foo()
}

fun doNotRunFoo() {
    inject val b = B()
    <!NO_CONTEXT_ARGUMENT!>foo<!>()
}

fun ambiguousFooContext() {
    context(A(), A()) {
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>foo<!>()
    }
}

fun ambiguousFooInject() {
    inject val a1 = A()
    inject val a2 = A()
    <!AMBIGUOUS_CONTEXT_ARGUMENT!>foo<!>()
}

fun runFooTwice() {
    inject val a1 = A()
    foo()
    inject val a2 = A()
    <!AMBIGUOUS_CONTEXT_ARGUMENT!>foo<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral,
localProperty, propertyDeclaration */
