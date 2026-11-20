// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CONFLICTING_INHERITED_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
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

interface IntersectionContextWithExtension : First, Second

interface IntersectionContextWithValue : First, Third

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver */
