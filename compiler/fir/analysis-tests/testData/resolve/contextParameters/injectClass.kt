// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A

context(a: A)
fun foo(): Int = 4

class Foo1(
    inject val a: A
) {
    fun bar() = foo()
}

class Foo2 {
    inject val a: A = A()

    fun bar() = foo()
}

class Foo3 {
    inject val a1: A = A()
    inject val a2: A = A()

    fun bar() = <!AMBIGUOUS_CONTEXT_ARGUMENT!>foo<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral,
primaryConstructor, propertyDeclaration */
