// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-44861
// DIAGNOSTICS: -UNUSED_VARIABLE

sealed class Foo() {
    class A : Foo()
    class B : Foo()
}

fun Foo(kind: String = "A"): Foo = when (kind) {
    "A" -> Foo.A()
    "B" -> Foo.B()
    else -> throw Exception()
}

fun main() {
    val foo = Foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, localProperty, nestedClass,
primaryConstructor, propertyDeclaration, sealed, stringLiteral, whenExpression, whenWithSubject */
