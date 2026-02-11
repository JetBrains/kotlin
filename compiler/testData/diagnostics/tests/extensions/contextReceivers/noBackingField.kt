// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A)
val a = <!CONTEXT_RECEIVERS_WITH_BACKING_FIELD!>1<!>

context(A, B)
var b = <!CONTEXT_RECEIVERS_WITH_BACKING_FIELD!>2<!>

context(A, B)
val c get() = a() + b()

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
propertyDeclaration, propertyDeclarationWithContext */
