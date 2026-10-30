// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CONFLICTING_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A
class B

context(a: A)
fun foo() {}

fun A.foo() { }

context(a: A, b: B)
fun bar(){}

context(a: A)
fun B.bar(){}

context(a: A, b: B)
fun qux(){}

fun A.qux(b: B){}

context(a: A)
val b: String
    get() = ""

val A.b: String
    get() = ""

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
getter, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral */
