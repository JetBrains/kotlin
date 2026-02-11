// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class A
class B

fun expectAB(f: context(A, B) () -> Unit) {
    f(A(), B())
}

fun test() {
    val l: context(B, A) () -> Unit = { }
    expectAB(<!TYPE_MISMATCH!>l<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
propertyDeclaration, typeWithContext */
