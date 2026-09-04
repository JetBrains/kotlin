// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A

interface First {
    context(a: A)
    fun foo()

    context(a: A)
    val b: String
}

interface Second {
    fun A.foo()
    val A.b: String
}

interface Third {
    fun foo(a: A)
}

interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>IntersectionContextWithExtension<!> : First, Second

interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>IntersectionContextWithValue<!> : First, Third

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver */
