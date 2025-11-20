// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

interface A { }
open class B : A { }
class C : B() { }

context(a: A) fun foo2(b: C) = ""
context(b: A) fun foo2(a: B) = 1

fun example1() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = B(), b = C())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(a = C(), b = C())
    consume<Int>(foo2(a = B(), b = B()))
}

context(a: A) fun example2() {
    foo2(b = C())
    foo2(b = <!ARGUMENT_TYPE_MISMATCH!>B()<!>)

    consume<Int>(foo2(B()))
    consume<String>(foo2(C()))

    consume<Int>(foo2(a = B()))
}

fun <T> consume(t: T) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration */
