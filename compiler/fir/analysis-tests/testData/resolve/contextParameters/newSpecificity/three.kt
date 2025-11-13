// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// FIR_DUMP

interface A { }
open class B : A { }
class C : B() { }

context(a: A) fun foo2(b: C) { }
context(b: A) fun foo2(a: B) { }

fun example1() {
    foo2(a = B(), b = C())
    foo2(a = C(), b = C())
    foo2(a = B(), b = B())
}

context(a: A) fun example2() {
    foo2(b = C())
    foo2(b = <!ARGUMENT_TYPE_MISMATCH!>B()<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration */
