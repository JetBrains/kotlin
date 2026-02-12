// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

<!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>context<!>(A)
val a = 1

<!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>context<!>(A, B)
var b = 2

context(A, B)
val c get() = <!NO_CONTEXT_ARGUMENT!>a<!>() + <!NO_CONTEXT_ARGUMENT, NO_CONTEXT_ARGUMENT!>b<!>()

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
propertyDeclaration, propertyDeclarationWithContext */
