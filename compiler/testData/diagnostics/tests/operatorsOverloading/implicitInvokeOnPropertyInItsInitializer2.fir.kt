// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED, -CONTEXT_CLASS_OR_CONSTRUCTOR
// LANGUAGE: +ContextReceivers, -ContextParameters
// WITH_STDLIB

interface C

fun C.foo(body: () -> Unit) {}

context(C)
class A {
    val foo = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!> {}
}

fun C.test() {
    object {
        val foo = foo {}
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, interfaceDeclaration, lambdaLiteral, propertyDeclaration */
